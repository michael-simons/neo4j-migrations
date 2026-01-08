/*
 * Copyright 2020-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ac.simons.neo4j.migrations.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.testcontainers.DockerClientFactory;

/**
 * Skips the tests if it is running on an aarch64 (Apple Silicon / M1) architecture and no
 * suitable image is available. It does *not* skip the tests on general arm64
 * architectures.
 *
 * @author Gerrit Meier
 */
final class SkipArm64IncompatibleConfiguration implements InvocationInterceptor {

	public static final boolean TEST_ONLY_LATEST_NEO_4_J = Boolean
		.parseBoolean(System.getProperty("migrations.test-only-latest-neo4j", "false"));

	private static final List<String> SUPPORTED_VERSIONS_COMMUNITY = List.of("3.5", "4.0", "4.1", "4.2", "4.3", "4.4",
			"5", "LATEST");

	private static final List<String> SUPPORTED_VERSIONS_ENTERPRISE = List.of("4.4", "5", "LATEST");

	private static boolean skipUnsupported(String argument) {
		if (!argument.toLowerCase(Locale.ROOT).startsWith("neo4j:")) {
			return false;
		}
		String version = argument.substring(argument.indexOf(":") + 1);
		if (argument.contains("enterprise")) {
			return !SUPPORTED_VERSIONS_ENTERPRISE.contains(version.replace("-enterprise", ""));
		}

		return SUPPORTED_VERSIONS_COMMUNITY.stream().noneMatch(version::startsWith);
	}

	private static boolean skipUnsupported(VersionUnderTest argument) {

		List<String> versionsToCheck;
		if (argument.enterprise) {
			versionsToCheck = SUPPORTED_VERSIONS_ENTERPRISE;
		}
		else {
			versionsToCheck = SUPPORTED_VERSIONS_COMMUNITY;
		}

		return versionsToCheck.stream().map(Neo4jVersion::of).noneMatch(argument.getNeo4jVersion()::equals);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		if (runsOnLocalAarch64Machine()) {
			skipUnsupportedTests(invocation, invocationContext);
		}
		else {
			invocation.proceed();
		}
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		if (runsOnLocalAarch64Machine()) {
			skipUnsupportedTests(invocation, invocationContext);
		}
		else {
			invocation.proceed();
		}
	}

	private boolean runsOnLocalAarch64Machine() {
		String dockerArchitecture = DockerClientFactory.instance().getInfo().getArchitecture();
		return "aarch64".equals(dockerArchitecture);
	}

	private void skipUnsupportedTests(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
		boolean skip = false;
		for (Object argument : invocationContext.getArguments()) {
			if (argument instanceof VersionUnderTest versionUnderTest) {
				skip = skipUnsupported(versionUnderTest);
			}
			else if (argument instanceof String versionUnderTest) {
				skip = skipUnsupported(versionUnderTest);
			}
			if (skip) {
				invocation.skip();
				return;
			}
		}

		invocation.proceed();
	}

	/**
	 * Version provider to iterate over all versions provided in {@link Neo4jVersion}.
	 */
	public static class VersionProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters,
				ExtensionContext context) {
			if (TEST_ONLY_LATEST_NEO_4_J) {
				return Stream.of(Arguments.of(new VersionUnderTest(Neo4jVersion.LATEST, true)));
			}
			// The exclusion of 4.0 and 4.1 here affects not every test ofc, only the ones
			// that rely on the provider for matrix tests. 4.0 and 4.1 are EOL since July
			// 2021 and December 2021 respectively.
			EnumSet<Neo4jVersion> unsupported = EnumSet.of(Neo4jVersion.V4_0, Neo4jVersion.V4_1,
					Neo4jVersion.UNDEFINED);
			return Arrays.stream(Neo4jVersion.values())
				.filter(version -> !(unsupported.contains(version)))
				.map(version -> Arguments.of(new VersionUnderTest(version, true)));
		}

	}

	/**
	 * Wrapper class for {@link Neo4jVersion} and a flag to determine if enterprise
	 * edition was requested.
	 */
	public static class VersionUnderTest {

		private final String neo4jVersion;

		final boolean enterprise;

		VersionUnderTest(Neo4jVersion value, boolean enterprise) {
			this.neo4jVersion = value.toString();
			this.enterprise = enterprise;
		}

		public String asTag() {
			return String.format("neo4j:%s%s", this.neo4jVersion, this.enterprise ? "-enterprise" : "");
		}

		@Override
		public String toString() {
			return this.neo4jVersion + (this.enterprise ? " (enterprise)" : "");
		}

		public Neo4jVersion getNeo4jVersion() {
			return Neo4jVersion.of(this.neo4jVersion);
		}

	}

}
