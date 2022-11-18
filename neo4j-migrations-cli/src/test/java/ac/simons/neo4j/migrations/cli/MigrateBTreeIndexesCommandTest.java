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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.ArgumentCaptor;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.MigrateBTreeIndexes;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;

/**
 * @author Michael J. Simons
 */
class MigrateBTreeIndexesCommandTest {

	private final ArgumentCaptor<MigrateBTreeIndexes> captor = ArgumentCaptor.forClass(MigrateBTreeIndexes.class);

	@Test
	void shouldDefaultToNotReplace() throws Exception {

		Migrations migrations = mock(Migrations.class);
		when(migrations.apply(any(Refactoring.class))).thenReturn(mock(Counters.class));

		MigrateBTreeIndexesCommand cmd = new MigrateBTreeIndexesCommand();
		cmd.withMigrations(migrations);

		verify(migrations).apply(captor.capture());

		MigrateBTreeIndexes refactoring = captor.getValue();
		Field dropOldIndexesField = refactoring.getClass().getDeclaredField("dropOldIndexes");
		dropOldIndexesField.setAccessible(true);

		boolean dropOldIndexes = (boolean) dropOldIndexesField.get(refactoring);
		assertThat(dropOldIndexes).isFalse();
	}

	@Test
	void shouldConfigureSuffix() throws Exception {

		Migrations migrations = mock(Migrations.class);
		when(migrations.apply(any(Refactoring.class))).thenReturn(mock(Counters.class));

		MigrateBTreeIndexesCommand cmd = new MigrateBTreeIndexesCommand();
		setValue(cmd, "suffix", "bar");
		cmd.withMigrations(migrations);

		verify(migrations).apply(captor.capture());

		MigrateBTreeIndexes refactoring = captor.getValue();

		Field field = refactoring.getClass().getDeclaredField("suffix");
		field.setAccessible(true);
		String suffix = (String) field.get(refactoring);
		assertThat(suffix).isEqualTo("bar");
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldApplyOptions() throws Exception {

		Migrations migrations = mock(Migrations.class);
		when(migrations.apply(any(Refactoring.class))).thenReturn(mock(Counters.class));

		MigrateBTreeIndexesCommand cmd = new MigrateBTreeIndexesCommand();
		setValue(cmd, "replace", true);
		setValue(cmd, "excludes", new HashSet<>(Arrays.asList("a", "b")));
		setValue(cmd, "includes", new HashSet<>(Arrays.asList("c", "d")));
		setValue(cmd, "mappings", Collections.singletonMap("aPoint", MigrateBTreeIndexesCommand.IndexType.POINT));
		cmd.withMigrations(migrations);

		verify(migrations).apply(captor.capture());

		MigrateBTreeIndexes refactoring = captor.getValue();

		Field field;

		field = refactoring.getClass().getDeclaredField("dropOldIndexes");
		field.setAccessible(true);
		boolean dropOldIndexes = (boolean) field.get(refactoring);
		assertThat(dropOldIndexes).isTrue();

		field = refactoring.getClass().getDeclaredField("excludes");
		field.setAccessible(true);
		Set<String> excludes = (Set<String>) field.get(refactoring);
		assertThat(excludes).containsExactly("a", "b");

		field = refactoring.getClass().getDeclaredField("includes");
		field.setAccessible(true);
		Set<String> includes = (Set<String>) field.get(refactoring);
		assertThat(includes).containsExactly("c", "d");

		field = refactoring.getClass().getDeclaredField("typeMapping");
		field.setAccessible(true);
		Map<String, Index.Type> typeMapping = (Map<String, Index.Type>) field.get(refactoring);
		assertThat(typeMapping).containsEntry("aPoint", Index.Type.POINT);
	}

	private static void setValue(MigrateBTreeIndexesCommand cmd, String field, Object value) {
		ReflectionSupport
			.findFields(MigrateBTreeIndexesCommand.class, f -> f.getName().equals(field), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, value);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			});
	}
}
