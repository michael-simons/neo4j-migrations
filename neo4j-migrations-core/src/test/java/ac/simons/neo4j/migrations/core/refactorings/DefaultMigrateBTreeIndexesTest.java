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
package ac.simons.neo4j.migrations.core.refactorings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.ItemType;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class DefaultMigrateBTreeIndexesTest {

	@Test
	void shouldFailProperOnNullMigrate() {

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
		assertThatIllegalArgumentException().isThrownBy(() -> refactoring.migrate(null))
			.withMessage("Cannot migrate null items");
	}

	@Test
	void shouldFailProperOnInvalidMigrate() {

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
		CatalogItem<?> wrongItem = new CatalogItem<>() {
			@Override public Name getName() {
				return Name.of("whatever");
			}

			@Override public ItemType getType() {
				return null;
			}

			@Override public boolean hasGeneratedName() {
				return false;
			}
		};
		assertThatIllegalArgumentException().isThrownBy(() -> refactoring.migrate(wrongItem))
			.withMessageStartingWith("Cannot migrate catalog items of type class ");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldMigrateConstraints(boolean dropOldIndexes) {

		String oldName = "book_isbn_unique";
		Constraint constraint = Constraint.forNode("Book").named(oldName).unique("isbn");

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(dropOldIndexes, null,
			Collections.emptyMap(), Collections.emptyList());
		CatalogItem<?> item = refactoring.migrate(constraint);
		assertThat(item.getName()).extracting(Name::getValue)
			.isEqualTo(dropOldIndexes ? oldName : "book_isbn_unique_new");
		assertThat(((Constraint) item).getOptionalOptions()).hasValue("{`indexProvider`: \"range-1.0\"}");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldMigrateIndexes(boolean dropOldIndexes) {

		String oldName = "book_isbn_idx";
		Index index = Index.forNode("Book").named(oldName).onProperties("isbn");

		DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(dropOldIndexes, null,
			Collections.emptyMap(), Collections.emptyList());
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
		assertThat(((Map<String, Index.Type>) typeMapping.get(m1)))
			.hasSize(1)
			.containsEntry("a", Index.Type.TEXT);
	}

	@Test
	void shouldIgnoreSameIgnores() {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes();
		MigrateBTreeIndexes m2 = m1.withIgnoredItems(null);
		MigrateBTreeIndexes m3 = m2.withIgnoredItems(Collections.emptySet());
		MigrateBTreeIndexes m4 = m2.withIgnoredItems(Collections.emptyList());

		assertThat(m2).isSameAs(m1);
		assertThat(m3).isSameAs(m2);
		assertThat(m4).isSameAs(m3);
	}

	@Test
	@SuppressWarnings({ "unchecked", "squid:S3011" })
	void shouldConfigureIgnores() throws IllegalAccessException, NoSuchFieldException {

		MigrateBTreeIndexes m1 = MigrateBTreeIndexes.replaceBTreeIndexes()
			.withIgnoredItems(Collections.singleton("a"));

		Field ignoredItems = DefaultMigrateBTreeIndexes.class.getDeclaredField("ignoredItems");
		ignoredItems.setAccessible(true);
		assertThat(((Set<String>) ignoredItems.get(m1))).containsExactly("a");
	}
}
