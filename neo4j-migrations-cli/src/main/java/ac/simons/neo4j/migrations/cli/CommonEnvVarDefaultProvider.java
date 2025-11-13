/*
 * Copyright 2020-2025 the original author or authors.
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
package ac.simons.neo4j.migrations.cli;

import java.util.Optional;
import java.util.Properties;

import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.PropertiesDefaultProvider;

/**
 * Neo4j seems to have settled on a common set of env variables across docs. This value
 * provider uses them if present for defaults.
 *
 * @author Michael J. Simons
 * @since 1.9.0
 */
final class CommonEnvVarDefaultProvider extends PropertiesDefaultProvider implements IDefaultValueProvider {

	private static final String ENV_ADDRESS = "NEO4J_URI";

	private static final String ENV_USERNAME = "NEO4J_USERNAME";

	private static final String ENV_PASSWORD = "NEO4J_PASSWORD";

	@SuppressWarnings("unused")
	CommonEnvVarDefaultProvider() {
	}

	CommonEnvVarDefaultProvider(Properties properties) {
		super(properties);
	}

	static Optional<String> getFromEnv(OptionSpec optionSpec) {
		return switch (optionSpec.shortestName()) {
			case MigrationsCli.OPTION_ADDRESS -> getOptionalEnv(ENV_ADDRESS);
			case MigrationsCli.OPTION_USERNAME -> getOptionalEnv(ENV_USERNAME);
			case MigrationsCli.OPTION_PASSWORD -> getOptionalEnv(ENV_PASSWORD);
			default -> Optional.empty();
		};
	}

	static Optional<String> getOptionalEnv(String name) {
		return Optional.ofNullable(System.getenv(name));
	}

	@Override
	public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {

		Optional<String> value = Optional.empty();
		if (argSpec instanceof OptionSpec optionSpec) {
			value = getFromEnv(optionSpec);
		}

		if (value.isPresent()) {
			return value.get();
		}
		return super.defaultValue(argSpec);
	}

}
