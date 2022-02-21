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

/**
 * @author Michael J. Simons
 */
class LoggingTest {

	@Test
	void shouldLogToSysOut() throws Exception {

		String result = tapSystemOut(() -> {
			MigrationsCli.LOGGER.info("Test");
			System.out.flush();
		});
		assertThat(result).isEqualTo("Test" + System.lineSeparator());
	}

	@Test // GH-421
	void shouldCatchAndLogMigrationsException() throws Exception {

		MigrationsCli cli = mock(MigrationsCli.class);
		when(cli.getAuthToken()).thenReturn(mock(AuthToken.class));
		when(cli.getConfig()).thenReturn(MigrationsConfig.builder().build());
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

		String result = tapSystemOut(() -> {
			cmd.call();
			System.out.flush();
		});
		assertThat(result).isEqualTo("Oh wie schade." + System.lineSeparator());
	}
}
