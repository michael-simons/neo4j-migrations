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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

import ac.simons.neo4j.migrations.core.DeleteResult;
import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;

/**
 * @author Michael J. Simons
 */
class DeleteCommandTest {

	@ParameterizedTest
	@ValueSource(strings = {"4711", "V5000__WithCommentAtEnd.cypher"})
	void shouldInvokeCorrectApi(String versionOrName) {

		Migrations migrations = mock(Migrations.class);
		DeleteResult result = mock(DeleteResult.class);

		when(result.prettyPrint()).thenReturn("deleted");
		when(result.getWarnings()).thenReturn(Collections.singletonList("a warning"));
		when(migrations.delete(any(MigrationVersion.class))).thenReturn(result);

		DeleteCommand cmd = new DeleteCommand();
		ReflectionSupport.findFields(DeleteCommand.class, f -> f.getName().equals("versionValue"),
				HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, versionOrName);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			});

		cmd.withMigrations(migrations);

		verify(migrations).delete(any(MigrationVersion.class));
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}
}
