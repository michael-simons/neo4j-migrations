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
package ac.simons.neo4j.migrations.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ac.simons.neo4j.migrations.core.Defaults;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class InitCommandTests {

	enum Mode {

		NO_GITIGNORE, OTHER_GITIGNORE, CONTAINS_GITIGNORE

	}

	@ParameterizedTest
	@EnumSource(Mode.class)
	void shouldInitDirectory(Mode mode) throws IOException {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--username", "bert");

		var propertiesPath = Paths.get(MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);
		Files.deleteIfExists(propertiesPath);

		var gitignorePath = propertiesPath.toAbsolutePath().getParent().resolve(".gitignore");
		var f = Files.deleteIfExists(gitignorePath);
		System.out.println("DDD " + f);
		var dir = Paths.get(Defaults.LOCATIONS_TO_SCAN_WITHOUT_PREFIX);
		if (Files.exists(dir)) {
			MigrationsCliTests.deltree(dir);
		}

		Set<String> lines = new TreeSet<>();
		if (mode == Mode.OTHER_GITIGNORE) {
			lines.addAll(List.of("a", "b"));
			Files.write(gitignorePath, lines);

		}
		else if (mode == Mode.CONTAINS_GITIGNORE) {
			lines.addAll(List.of("a", MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME, "b"));
			Files.write(gitignorePath, lines);
		}
		lines.add(MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);

		try {
			var cmd = commandLine.getSubcommands().get("init");
			cmd.execute();

			assertThat(Files.isRegularFile(propertiesPath)).isTrue();
			assertThat(Files.isDirectory(dir)).isTrue();

			var gitignore = propertiesPath.toAbsolutePath().getParent().resolve(".gitignore");
			assertThat(Files.exists(gitignore)).isTrue();

			var actual = Files.readAllLines(gitignore);
			assertThat(actual).containsExactlyInAnyOrderElementsOf(lines);

			var optionalProperties = MigrationsCli.loadProperties(MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);
			assertThat(optionalProperties).isPresent()
				.hasValueSatisfying(p -> assertThat(p).containsEntry("username", "bert"));
		}
		finally {
			Files.delete(propertiesPath);
			MigrationsCliTests.deltree(dir);
		}
	}

}
