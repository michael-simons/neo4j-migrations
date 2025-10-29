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

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMigrateBTreeIndexesIT {

	private static final String QUERY_GET_OLD_CONSTRAINTS = "SHOW CONSTRAINTS YIELD name, options WHERE name =~ 'm_.*' AND options['indexProvider'] = 'native-btree-1.0'";
	private static final String QUERY_GET_NEW_CONSTRAINTS = "SHOW CONSTRAINTS YIELD name, options WHERE name =~ 'm_.*' AND options['indexProvider'] = 'range-1.0'";
	private static final String QUERY_GET_OLD_INDEXES = "SHOW INDEXES YIELD name, type, uniqueness WHERE name =~ 'm_.*' AND type = 'BTREE' AND uniqueness <> 'UNIQUE'";
	private static final String QUERY_GET_NEW_INDEXES = "SHOW INDEXES YIELD name, type, uniqueness WHERE name =~ 'm_.*' AND type = 'RANGE' AND uniqueness <> 'UNIQUE'";
	private static final Function<Record, String> EXTRACT_NAME = r -> r.get("name").asString();

	@SuppressWarnings("resource")
	protected final Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4-enterprise")
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	@BeforeAll
	void startNeo4j() {
		neo4j.start();
	}

	Driver getDriver() {

		return GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()));
	}

	@BeforeEach
	void prepareDatabase() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			session.run("SHOW constraints YIELD name").list(EXTRACT_NAME)
				.forEach(n -> session.run("DROP constraint " + n));

			session.run("SHOW indexes YIELD *  WHERE type <> 'LOOKUP' RETURN name").list(EXTRACT_NAME)
				.forEach(n -> session.run("DROP index " + n));

			for (String stmt : new String[] {
				"CREATE CONSTRAINT m_default_unique_constraint FOR (book:A) REQUIRE book.isbn IS UNIQUE",
				"CREATE CONSTRAINT m_default_node_key_constraint FOR (n:D) REQUIRE (n.firstname, n.surname) IS NODE KEY",
				"CREATE INDEX m_default_index FOR (n:E) ON (n.surname)",
				"CREATE INDEX m_default_composite_index FOR (n:F) ON (n.age, n.country)",
				"CREATE INDEX m_location_index FOR (n:P) ON (n.location)",
				"CREATE CONSTRAINT u_default_existential_constraint FOR (book:C) REQUIRE book.isbn IS NOT NULL",
				"CREATE CONSTRAINT u_constraint_with_new_index FOR (book:B) REQUIRE book.isbn IS UNIQUE options {indexProvider: 'range-1.0'}",
				"CREATE CONSTRAINT u_node_key_constraint_with_new_index FOR (n:D) REQUIRE (n.a, n.b) IS NODE KEY OPTIONS {indexProvider: 'range-1.0'}",
				"CREATE RANGE INDEX u_node_range_index_name FOR (n:G) ON (n.surname)",
				"CREATE RANGE INDEX u_node_range_index_name_options FOR (n:H) ON (n.surname) OPTIONS {indexProvider: 'range-1.0'}"
			}) {
				session.run(stmt).consume();
			}
		}
	}

	@Test
	void shouldFindIndexes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
			Map<Long, Index> bTreeBasedIndexes = refactoring.findBTreeBasedIndexes(session::run);
			assertThat(bTreeBasedIndexes.values())
				.hasSize(5)
				.extracting(Index::getName)
				.extracting(Name::getValue)
				.containsExactlyInAnyOrder(
					"m_default_unique_constraint",
					"m_default_node_key_constraint",
					"m_default_index",
					"m_default_composite_index",
					"m_location_index"
				);
		}
	}

	@Test
	void shouldFindConstraints() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
			Map<Long, Constraint> constraints = refactoring.findBTreeBasedConstraints(session::run, null);
			assertThat(constraints.values())
				.hasSize(2)
				.extracting(Constraint::getName)
				.extracting(Name::getValue)
				.containsExactlyInAnyOrder(
					"m_default_unique_constraint",
					"m_default_node_key_constraint"
				);
		}
	}

	@Test
	void shouldFindAllItems() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
			List<CatalogItem<?>> items = refactoring.findBTreeBasedItems(session::run);
			assertThat(items)
				.hasSize(5)
				.satisfies(item -> assertThat(item).isInstanceOf(Constraint.class),
					org.assertj.core.data.Index.atIndex(0))
				.satisfies(item -> assertThat(item).isInstanceOf(Constraint.class),
					org.assertj.core.data.Index.atIndex(1))
				.satisfies(item -> assertThat(item).isInstanceOf(Index.class), org.assertj.core.data.Index.atIndex(2))
				.satisfies(item -> assertThat(item).isInstanceOf(Index.class), org.assertj.core.data.Index.atIndex(3))
				.satisfies(item -> assertThat(item).isInstanceOf(Index.class), org.assertj.core.data.Index.atIndex(4));
		}
	}

	@Test
	void shouldCreateParallelIndexes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes();
			Counters counters = refactoring.apply(new TestRefactoringContext(session));

			assertThat(counters.constraintsAdded()).isEqualTo(2);
			assertThat(counters.constraintsRemoved()).isZero();
			assertThat(counters.indexesAdded()).isEqualTo(3);
			assertThat(counters.indexesRemoved()).isZero();

			List<String> oldConstraints = session
				.run(QUERY_GET_OLD_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(oldConstraints).containsExactlyInAnyOrder("m_default_unique_constraint",
				"m_default_node_key_constraint");

			List<String> newConstraints = session
				.run(QUERY_GET_NEW_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(newConstraints).containsExactlyInAnyOrder("m_default_unique_constraint_new",
				"m_default_node_key_constraint_new");

			List<String> oldIndexes = session
				.run(QUERY_GET_OLD_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(oldIndexes).containsExactlyInAnyOrder("m_default_index", "m_default_composite_index", "m_location_index");

			List<String> newIndexes = session
				.run(QUERY_GET_NEW_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(newIndexes).containsExactlyInAnyOrder("m_default_index_new", "m_default_composite_index_new", "m_location_index_new");
		}
	}

	@Test
	void shouldUseExcludes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(true, null, Collections.emptyMap(),
				Arrays.asList("m_default_unique_constraint", "m_default_index"), Collections.emptyList());
			Counters counters = refactoring.apply(new TestRefactoringContext(session));

			assertThat(counters.constraintsAdded()).isEqualTo(1);
			assertThat(counters.constraintsRemoved()).isEqualTo(1);
			assertThat(counters.indexesAdded()).isEqualTo(2);
			assertThat(counters.indexesRemoved()).isEqualTo(2);

			List<String> oldConstraints = session
				.run(QUERY_GET_OLD_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(oldConstraints).containsExactlyInAnyOrder("m_default_unique_constraint");

			List<String> newConstraints = session
				.run(QUERY_GET_NEW_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(newConstraints).containsExactlyInAnyOrder("m_default_node_key_constraint");

			List<String> oldIndexes = session
				.run(QUERY_GET_OLD_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(oldIndexes).containsExactlyInAnyOrder("m_default_index");

			List<String> newIndexes = session
				.run(QUERY_GET_NEW_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(newIndexes).containsExactlyInAnyOrder("m_default_composite_index", "m_location_index");
		}
	}

	@Test
	void shouldUseIncludes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(true, null, Collections.emptyMap(),
				Collections.emptyList(), Arrays.asList("m_default_unique_constraint", "m_default_index"));
			Counters counters = refactoring.apply(new TestRefactoringContext(session));

			assertThat(counters.constraintsAdded()).isEqualTo(1);
			assertThat(counters.constraintsRemoved()).isEqualTo(1);
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(1);

			List<String> oldConstraints = session
				.run(QUERY_GET_OLD_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(oldConstraints).containsExactlyInAnyOrder("m_default_node_key_constraint");

			List<String> newConstraints = session
				.run(QUERY_GET_NEW_CONSTRAINTS)
				.list(EXTRACT_NAME);
			assertThat(newConstraints).containsExactlyInAnyOrder("m_default_unique_constraint");

			List<String> oldIndexes = session
				.run(QUERY_GET_OLD_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(oldIndexes).containsExactlyInAnyOrder("m_default_composite_index", "m_location_index");

			List<String> newIndexes = session
				.run(QUERY_GET_NEW_INDEXES)
				.list(EXTRACT_NAME);
			assertThat(newIndexes).containsExactlyInAnyOrder("m_default_index");
		}
	}

	@Test
	void shouldDropIndexes() {

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(true, null, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
			Counters counters = refactoring.apply(new TestRefactoringContext(session));

			assertThat(counters.constraintsAdded()).isEqualTo(2);
			assertThat(counters.constraintsRemoved()).isEqualTo(2);
			assertThat(counters.indexesAdded()).isEqualTo(3);
			assertThat(counters.indexesRemoved()).isEqualTo(3);

			List<String> oldConstraints = session.run(QUERY_GET_OLD_CONSTRAINTS).list(EXTRACT_NAME);
			assertThat(oldConstraints).isEmpty();

			List<String> newConstraints = session.run(QUERY_GET_NEW_CONSTRAINTS).list(EXTRACT_NAME);
			assertThat(newConstraints).containsExactlyInAnyOrder("m_default_unique_constraint", "m_default_node_key_constraint");

			List<String> oldIndexes = session.run(QUERY_GET_OLD_INDEXES).list(EXTRACT_NAME);
			assertThat(oldIndexes).isEmpty();

			List<String> newIndexes = session.run(QUERY_GET_NEW_INDEXES).list(EXTRACT_NAME);
			assertThat(newIndexes).containsExactlyInAnyOrder("m_default_index", "m_default_composite_index", "m_location_index");
		}
	}

	@Test
	void shouldUseTypeMapping() {

		Map<String, Index.Type> typeMapping = new HashMap<>();
		typeMapping.put("m_default_index", Index.Type.TEXT);
		typeMapping.put("m_location_index", Index.Type.POINT);

		try (Driver driver = getDriver(); Session session = driver.session()) {
			DefaultMigrateBTreeIndexes refactoring = new DefaultMigrateBTreeIndexes(true, null, typeMapping, Collections.emptyList(), Collections.emptyList());
			Counters counters = refactoring.apply(new TestRefactoringContext(session));

			assertThat(counters.constraintsAdded()).isEqualTo(2);
			assertThat(counters.constraintsRemoved()).isEqualTo(2);
			assertThat(counters.indexesAdded()).isEqualTo(3);
			assertThat(counters.indexesRemoved()).isEqualTo(3);

			List<String> oldConstraints = session.run(QUERY_GET_OLD_CONSTRAINTS).list(EXTRACT_NAME);
			assertThat(oldConstraints).isEmpty();

			List<String> newConstraints = session.run(QUERY_GET_NEW_CONSTRAINTS).list(EXTRACT_NAME);
			assertThat(newConstraints).containsExactlyInAnyOrder("m_default_unique_constraint", "m_default_node_key_constraint");

			List<String> oldIndexes = session.run(QUERY_GET_OLD_INDEXES).list(EXTRACT_NAME);
			assertThat(oldIndexes).isEmpty();

			Map<String, String> newIndexes = session
				.run("SHOW INDEXES YIELD name, type, uniqueness WHERE name =~ 'm_.*' AND type <> 'BTREE' AND uniqueness <> 'UNIQUE'")
				.stream()
				.collect(Collectors.toMap(r -> r.get("name").asString(), r -> r.get("type").asString()));
			assertThat(newIndexes)
				.hasSize(3)
				.containsEntry("m_default_index", "TEXT")
				.containsEntry("m_location_index", "POINT")
				.containsEntry("m_default_composite_index", "RANGE");
		}
	}

	private static class TestRefactoringContext implements RefactoringContext {

		private final Session session;

		TestRefactoringContext(Session session) {
			this.session = session;
		}

		@Override
		public Optional<String> findSingleResultIdentifier(String query) {
			return Optional.empty();
		}

		@Override
		public QueryRunner getQueryRunner(QueryRunner.FeatureSet featureSet) {
			return session::run;
		}
	}
}
