/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.maven;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;

import org.junit.Test;

/**
 * @author Michael J. Simons
 */
public class InfoMojoTest {

	@Test
	public void shouldLog() {

		Migrations migrations = mock(Migrations.class);
		MigrationChain info = mock(MigrationChain.class);
		when(info.prettyPrint()).thenReturn("<<info>>");
		when(migrations.info()).thenReturn(info);

		InfoMojo cmd = new InfoMojo();
		cmd.withMigrations(migrations);

		verify(migrations).info();
		verify(info).prettyPrint();

		verifyNoMoreInteractions(migrations, info);
	}
}
