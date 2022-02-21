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
package ac.simons.neo4j.migrations.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.Defaults;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class InitCommandTest {

	@Test
	void shouldInitDirectory() throws IOException {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--username", "bert");

		Path propertiesPath = Paths.get(MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);
		assertThat(Files.exists(propertiesPath)).isFalse();
		Path dir = Paths.get(Defaults.LOCATIONS_TO_SCAN_WITHOUT_PREFIX);
		assertThat(Files.exists(dir)).isFalse();

		try {
			CommandLine cmd = commandLine.getSubcommands().get("init");
			cmd.execute();

			assertThat(Files.isRegularFile(propertiesPath)).isTrue();
			assertThat(Files.isDirectory(dir)).isTrue();

			Optional<Properties> optionalProperties = MigrationsCli.loadProperties(
				MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);
			assertThat(optionalProperties)
				.isPresent()
				.hasValueSatisfying(p -> assertThat(p).containsEntry("username", "bert"));
		} finally {
			Files.delete(propertiesPath);
			MigrationsCliTest.deltree(dir);
		}
	}
}
