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

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMigrateBTreeToRangeIndexesTest {

	Driver getDriver() {

		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
	}

	@BeforeAll
	void prepareDatabase() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			session.run("SHOW constraints YIELD name")
				.list(r -> r.get("name").asString())
				.forEach(n -> session.run("DROP constraint " + n));

			session.run("SHOW indexes YIELD *  WHERE type <> 'LOOKUP' RETURN name")
				.list(r -> r.get("name").asString())
				.forEach(n -> session.run("DROP index " + n));

			for (String stmt : new String[] {
				"CREATE CONSTRAINT default_unique_constraint FOR (book:A) REQUIRE book.isbn IS UNIQUE",
				"CREATE CONSTRAINT default_node_key_constraint FOR (n:D) REQUIRE (n.firstname, n.surname) IS NODE KEY",
				"CREATE INDEX default_index FOR (n:E) ON (n.surname)",
				"CREATE INDEX default_composite_index FOR (n:F) ON (n.age, n.country)",
				"CREATE CONSTRAINT default_existential_constraint FOR (book:C) REQUIRE book.isbn IS NOT NULL",
				"CREATE CONSTRAINT constraint_with_new_index FOR (book:B) REQUIRE book.isbn IS UNIQUE options {indexProvider: 'range-1.0'}",
				"CREATE CONSTRAINT node_key_constraint_with_new_index FOR (n:D) REQUIRE (n.firstname, n.surname) IS NODE KEY OPTIONS {indexProvider: 'range-1.0'}",
				"CREATE RANGE INDEX node_range_index_name FOR (n:G) ON (n.surname)",
				"CREATE RANGE INDEX node_range_index_name_options FOR (n:H) ON (n.surname) OPTIONS {indexProvider: 'range-1.0'}"
			}) {
				session.run(stmt).consume();
			}
		}
	}

	@Test
	void shouldFindIndexes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeToRangeIndexes refactoring = new DefaultMigrateBTreeToRangeIndexes(false);
			Map<Long, Index> bTreeBasedIndexes = refactoring.findBTreeBasedIndexes(session::run);
			assertThat(bTreeBasedIndexes.values())
				.hasSize(4)
				.extracting(Index::getName)
				.extracting(Name::getValue)
				.containsExactlyInAnyOrder(
					"default_unique_constraint",
					"default_node_key_constraint",
					"default_index",
					"default_composite_index"
				);
		}
	}

	@Test
	void shouldFindConstraints() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeToRangeIndexes refactoring = new DefaultMigrateBTreeToRangeIndexes(false);
			Map<Long, Constraint> constraints = refactoring.findBTreeBasedConstraints(session::run, null);
			assertThat(constraints.values())
				.hasSize(2)
				.extracting(Constraint::getName)
				.extracting(Name::getValue)
				.containsExactlyInAnyOrder(
					"default_unique_constraint",
					"default_node_key_constraint"
				);

			constraints.values().stream().forEach(i -> {
				System.out.println(i.getOptionalOptions().get().replaceAll("`(.+?)`:", "\"$1\":"));
				JsonWheel.WheelNode read = JsonWheel.read(i.getOptionalOptions().get().replaceAll("`(.+?)`:", "\"$1\":"));
				System.out.println(read.val(Map.class));
			});

		}
	}

	@Test
	void shouldFindAllItems() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeToRangeIndexes refactoring = new DefaultMigrateBTreeToRangeIndexes(false);
			List<CatalogItem<?>> items = refactoring.findBTreeBasedItems(session::run);
			assertThat(items)
				.hasSize(4)
				.satisfies(item -> assertThat(item).isInstanceOf(Constraint.class), org.assertj.core.data.Index.atIndex(0))
				.satisfies(item -> assertThat(item).isInstanceOf(Constraint.class), org.assertj.core.data.Index.atIndex(1))
				.satisfies(item -> assertThat(item).isInstanceOf(Index.class), org.assertj.core.data.Index.atIndex(2))
				.satisfies(item -> assertThat(item).isInstanceOf(Index.class), org.assertj.core.data.Index.atIndex(3));
		}
	}
}
