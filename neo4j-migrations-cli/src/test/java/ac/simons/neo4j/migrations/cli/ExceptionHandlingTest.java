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
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ClientException;

/**
 * @author Michael J. Simons
 */
class ExceptionHandlingTest {

	@Test // GH-421
	void shouldCatchAndLogMigrationsException() throws Exception {

		String result = tapSystemOut(() -> {
			MigrationsCli cli = mock(MigrationsCli.class);
			when(cli.getAuthToken()).thenReturn(mock(AuthToken.class));
			when(cli.getConfig(false)).thenReturn(MigrationsConfig.builder().build());
			when(cli.openConnection(Mockito.any(AuthToken.class))).thenReturn(mock(Driver.class));

			ConnectedCommand cmd = new ConnectedCommand() {
				@Override
				MigrationsCli getParent() {
					return cli;
				}

				@Override Integer withMigrations(Migrations migrations) {
					throw new MigrationsException("Oh wie schade.");
				}
			};

			ConnectedCommand cmd2 = new ConnectedCommand() {
				@Override
				MigrationsCli getParent() {
					return cli;
				}

				@Override Integer withMigrations(Migrations migrations) {
					throw new MigrationsException("Could not apply migration: 0020 (\"Create unique movie title\").",
						new ClientException("Neo4j.YouMessedUp", "This was not valid."));
				}
			};

			cmd.call();
			cmd2.call();
			System.out.flush();
		});

		assertThat(result).isEqualTo("Oh wie schade." + System.lineSeparator()
			+ "Could not apply migration: 0020 (\"Create unique movie title\")."
			+ System.lineSeparator() + "\tNeo4j.YouMessedUp: This was not valid."
			+ System.lineSeparator());
	}
}
