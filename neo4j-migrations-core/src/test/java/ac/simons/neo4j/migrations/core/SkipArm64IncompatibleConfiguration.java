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

import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.testcontainers.DockerClientFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Skips the tests if it is running on an aarch64 (Apple silicon / M1) architecture and no suitable image is available.
 * It does *not* skip the tests on general arm64 architectures.
 */
public class SkipArm64IncompatibleConfiguration implements InvocationInterceptor {

	/**
	 * Version provider to iterate over all versions provided in {@link Neo4jVersion}.
	 */
	public static class VersionProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return Arrays.stream(Neo4jVersion.values())
					.filter(version -> version != Neo4jVersion.LATEST && version != Neo4jVersion.UNDEFINED)
					.map(version -> Arguments.of(new VersionUnderTest(version, true)));
		}
	}

	/**
	 * Wrapper class for {@link Neo4jVersion} and a flag to determine if enterprise edition was requested.
	 */
	public static class VersionUnderTest {
		final Neo4jVersion version;
		final boolean enterprise;

		VersionUnderTest(Neo4jVersion version, boolean enterprise) {
			this.version = version;
			this.enterprise = enterprise;
		}
	}
	private final List<String> SUPPORTED_VERSIONS_COMMUNITY = Arrays.asList("3.5", "4.1", "4.2", "4.3", "4.4");
	private final List<String> SUPPORTED_VERSIONS_ENTERPRISE = Arrays.asList("4.4");

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
				skip = checkForVersionArguments((VersionUnderTest) argument);
			}
			if (argument instanceof String) {
				skip = checkForStringArguments((String) argument);
			}
		}
		if (skip) {
			invocation.skip();
		} else {
			invocation.proceed();
		}
	}

	private boolean checkForStringArguments(String argument) {
		if (argument.contains("enterprise")) {
			return !SUPPORTED_VERSIONS_ENTERPRISE.contains(argument.replace("-enterprise", ""));
		}
		for (String supportedVersion : SUPPORTED_VERSIONS_COMMUNITY) {
			if (argument.contains(supportedVersion)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkForVersionArguments(VersionUnderTest argument) {
		if (argument.enterprise) {
			for (String supportedVersion : SUPPORTED_VERSIONS_ENTERPRISE) {
				Neo4jVersion neo4jVersion = Neo4jVersion.of(supportedVersion);
				if (argument.version.equals(neo4jVersion)) {
					return false;
				}
			}
			return true;
		}

		for (String supportedVersion : SUPPORTED_VERSIONS_COMMUNITY) {
			Neo4jVersion neo4jVersion = Neo4jVersion.of(supportedVersion);
			if (argument.version.equals(neo4jVersion)) {
				return false;
			}
		}
		return true;
	}
}
