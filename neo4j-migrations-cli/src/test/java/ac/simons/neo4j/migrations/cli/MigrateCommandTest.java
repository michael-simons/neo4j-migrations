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

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrateCommandTest {

	@Test
	void shouldGiveInformationAboutMigrations() throws Exception {

		// Done in one test, because otherwise tapping sysout goes upside down. No mood to bother too long with this as of writing.
		String result = tapSystemOut(() -> {

			Migrations noMigrations = mock(Migrations.class);
			when(noMigrations.apply()).thenReturn(Optional.empty());

			Migrations migrations = mock(Migrations.class);
			MigrationVersion version = mock(MigrationVersion.class);
			when(version.getValue()).thenReturn("4711");
			when(migrations.apply()).thenReturn(Optional.of(version));

			MigrateCommand cmd = new MigrateCommand();
			cmd.withMigrations(noMigrations);
			cmd.withMigrations(migrations);
			System.out.flush();

			verify(noMigrations).apply();
			verify(migrations).apply();
		});
		assertThat(result).isEqualTo(
			"No migrations have been applied." + System.lineSeparator() + "Database migrated to version 4711."
			+ System.lineSeparator());
	}
}
