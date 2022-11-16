/*
 * Copyright 2020-2022 the original author or authors.
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.testcontainers.DockerClientFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Skips the tests if it is running on an aarch64 (Apple silicon / M1) architecture and no suitable image is available.
 * It does *not* skip the tests on general arm64 architectures.
 *
 * @author Gerrit Meier
 */
final class SkipArm64IncompatibleConfiguration implements InvocationInterceptor {

	/**
	 * Version provider to iterate over all versions provided in {@link Neo4jVersion}.
	 */
	public static class VersionProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			boolean testOnlyLatestNeo4j = Boolean.parseBoolean(System.getProperty("migrations.test-only-latest-neo4j", "false"));
			if (testOnlyLatestNeo4j) {
				return Stream.of(Arguments.of(new VersionUnderTest(Neo4jVersion.LATEST, true)));
			}
			// The exclusion of 4.0 and 4.1 here affects not every test ofc, only the ones
			// that rely on the provider for matrix tests. 4.0 and 4.1 are EOL since July 2021 and December 2021 respectively.
			EnumSet<Neo4jVersion> unsupported = EnumSet.of(Neo4jVersion.V4_0, Neo4jVersion.V4_1, Neo4jVersion.UNDEFINED);
			return Arrays.stream(Neo4jVersion.values())
					.filter(version -> !(unsupported.contains(version) || version == Neo4jVersion.LATEST))
					.map(version -> Arguments.of(new VersionUnderTest(version, true)));
		}
	}

	/**
	 * Wrapper class for {@link Neo4jVersion} and a flag to determine if enterprise edition was requested.
	 */
	public static class VersionUnderTest {
		final Neo4jVersion value;
		final boolean enterprise;

		VersionUnderTest(Neo4jVersion value, boolean enterprise) {
			this.value = value;
			this.enterprise = enterprise;
		}

		@Override
		public String toString() {
			return this.value.toString() + (enterprise ? " (enterprise)" : "");
		}
	}
	private static final List<String> SUPPORTED_VERSIONS_COMMUNITY = Collections.unmodifiableList(Arrays.asList("3.5", "4.0", "4.1", "4.2", "4.3", "4.4", "5", "LATEST"));
	private static final List<String> SUPPORTED_VERSIONS_ENTERPRISE = Collections.unmodifiableList(Arrays.asList("4.4", "5", "LATEST"));

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		if (runsOnLocalAarch64Machine()) {
			skipUnsupportedTests(invocation, invocationContext);
		} else {
			invocation.proceed();
		}
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		if (runsOnLocalAarch64Machine()) {
			skipUnsupportedTests(invocation, invocationContext);
		} else {
			invocation.proceed();
		}
	}

	private boolean runsOnLocalAarch64Machine() {
		String dockerArchitecture = DockerClientFactory.instance().getInfo().getArchitecture();
		return "aarch64".equals(dockerArchitecture);
	}

	private void skipUnsupportedTests(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
		boolean skip = false;
		for (Object argument : invocationContext.getArguments()) {
			if (argument instanceof VersionUnderTest) {
				skip = skipUnsupported((VersionUnderTest) argument);
			}
			if (argument instanceof String) {
				skip = skipUnsupported((String) argument);
			}
		}
		if (skip) {
			invocation.skip();
		} else {
			invocation.proceed();
		}
	}

	private static boolean skipUnsupported(String argument) {
		if (argument.contains("enterprise")) {
			return !SUPPORTED_VERSIONS_ENTERPRISE.contains(argument.replace("-enterprise", ""));
		}
		return SUPPORTED_VERSIONS_COMMUNITY.stream().noneMatch(argument::contains);
	}

	private static boolean skipUnsupported(VersionUnderTest argument) {

		List<String> versionsToCheck;
		if (argument.enterprise) {
			versionsToCheck = SUPPORTED_VERSIONS_ENTERPRISE;
		} else {
			versionsToCheck = SUPPORTED_VERSIONS_COMMUNITY;
		}

		return versionsToCheck
				.stream()
				.map(Neo4jVersion::of)
				.noneMatch(argument.value::equals);
	}
}
