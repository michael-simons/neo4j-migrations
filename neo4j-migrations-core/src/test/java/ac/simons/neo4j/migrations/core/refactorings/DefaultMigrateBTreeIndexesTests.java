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
package ac.simons.neo4j.migrations.core.refactorings;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.SomeCatalogItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class DefaultMigrateBTreeIndexesTests {

	@Test
	void shouldFailProperOnNullMigrate() {

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
		assertThatIllegalArgumentException().isThrownBy(() -> refactoring.migrate(null))
			.withMessage("Cannot migrate null items");
	}

	@Test
	void shouldFailProperOnInvalidMigrate() {

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
		assertThatIllegalArgumentException().isThrownBy(() -> refactoring.migrate(new SomeCatalogItem()))
			.withMessageStartingWith("Cannot migrate catalog items of type class ");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldMigrateConstraints(boolean dropOldIndexes) {

		String oldName = "book_isbn_unique";
		Constraint constraint = Constraint.forNode("Book").named(oldName).unique("isbn");

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(dropOldIndexes, null,
				Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
		CatalogItem<?> item = refactoring.migrate(constraint);
		assertThat(item.getName()).extracting(Name::getValue)
			.isEqualTo(dropOldIndexes ? oldName : "book_isbn_unique_new");
		assertThat(item.getOptionalOptions()).hasValue("{`indexProvider`: \"range-1.0\"}");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldMigrateIndexes(boolean dropOldIndexes) {

		String oldName = "book_isbn_idx";
		Index index = Index.forNode("Book").named(oldName).onProperties("isbn");

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(dropOldIndexes, null,
				Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
		CatalogItem<?> item = refactoring.migrate(index);
		assertThat(item.getName()).extracting(Name::getValue).isEqualTo(dropOldIndexes ? oldName : "book_isbn_idx_new");
		assertThat(((Index) item).getOptionalOptions()).hasValue("{`indexProvider`: \"range-1.0\"}");
	}

	@Test
	void shouldIgnoreSameTypeMappings() {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes();
		MigrateBTreeIndexes m2 = m1.withTypeMapping(null);
		MigrateBTreeIndexes m3 = m2.withTypeMapping(Collections.emptyMap());
		MigrateBTreeIndexes m4 = m2.withTypeMapping(new HashMap<>());

		assertThat(m2).isSameAs(m1);
		assertThat(m3).isSameAs(m2);
		assertThat(m4).isSameAs(m3);
	}

	@Test
	@SuppressWarnings({ "unchecked", "squid:S3011" })
	void shouldConfigureTypeMapping() throws IllegalAccessException, NoSuchFieldException {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes()
			.withTypeMapping(Collections.singletonMap("a", Index.Type.TEXT));

		Field typeMapping = DefaultMigrateBTreeIndexes.class.getDeclaredField("typeMapping");
		typeMapping.setAccessible(true);
		assertThat(((Map<String, Index.Type>) typeMapping.get(m1))).hasSize(1).containsEntry("a", Index.Type.TEXT);
	}

	@Test
	void shouldIgnoreSameExcludes() {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes();
		MigrateBTreeIndexes m2 = m1.withExcludes(null);
		MigrateBTreeIndexes m3 = m2.withExcludes(Collections.emptySet());
		MigrateBTreeIndexes m4 = m2.withExcludes(Collections.emptyList());

		assertThat(m2).isSameAs(m1);
		assertThat(m3).isSameAs(m2);
		assertThat(m4).isSameAs(m3);
	}

	@Test
	void shouldIgnoreSameIncludes() {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes();
		MigrateBTreeIndexes m2 = m1.withIncludes(null);
		MigrateBTreeIndexes m3 = m2.withIncludes(Collections.emptySet());
		MigrateBTreeIndexes m4 = m2.withIncludes(Collections.emptyList());

		assertThat(m2).isSameAs(m1);
		assertThat(m3).isSameAs(m2);
		assertThat(m4).isSameAs(m3);
	}

	@ParameterizedTest
	@ValueSource(strings = { "excludes", "includes" })
	@SuppressWarnings({ "unchecked", "squid:S3011" })
	void shouldConfigureExcludesAndIncludes(String field) throws IllegalAccessException, NoSuchFieldException {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes();
		MigrateBTreeIndexes m2 = MigrateBTreeIndexes.replaceBTreeIndexes();
		if ("excludes".equals(field)) {
			m1 = m1.withExcludes(Collections.singleton("a"));
			m2 = m2.withExcludes(Collections.singleton("a"));
		}
		else {
			m1 = m1.withIncludes(Collections.singleton("a"));
			m2 = m2.withIncludes(Collections.singleton("a"));
		}

		assertThat(m2).isEqualTo(m1);

		Field ignoredItems = DefaultMigrateBTreeIndexes.class.getDeclaredField(field);
		ignoredItems.setAccessible(true);
		assertThat(((Set<String>) ignoredItems.get(m1))).containsExactly("a");
	}

}
