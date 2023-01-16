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
package ac.simons.neo4j.migrations.cli;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.Migrations;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.Mockito;

/**
 * @author Michael J. Simons
 */
class RunCommandTest {

	@Test
	void shouldGiveInformationAboutMigrations() throws Exception {

		String result = tapSystemOut(() -> {

			Migrations migrations = mock(Migrations.class);
			when(migrations.apply(Mockito.any(URL.class))).thenReturn(23);

			RunCommand cmd = new RunCommand();
			setMigrations(cmd, new URL("file:///whatever"));
			cmd.withMigrations(migrations);
			System.out.flush();
			verify(migrations).apply(Mockito.any(URL.class));
		});
		assertThat(result).isEqualTo("Applied 23 migration(s)." + System.lineSeparator());
	}

	private static void setMigrations(RunCommand cmd, URL url) {
		ReflectionSupport
			.findFields(RunCommand.class, f -> f.getName().equals("migrationsToRun"), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, new URL[] { url });
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			});
	}
}
