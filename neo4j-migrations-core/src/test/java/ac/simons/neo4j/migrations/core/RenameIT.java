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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.RefactoringContext;
import ac.simons.neo4j.migrations.core.refactorings.Rename;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameIT extends AbstractRefactoringsITTestBase {

	@BeforeEach
	void clearDatabase() {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherResource.of(RenameIT.class.getResource("/moviegraph/movies.cypher")).getExecutableStatements()
				.forEach(session::run);
		}
	}

	@Test
	void shouldRenameLabels() {

		Rename rename = Rename.label("Movie", "Film");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatAllLabelsHaveBeenRenamed(session, counters);
		}
	}

	@Test
	void shouldRenameLabelsNotExecutingThings() {

		Rename rename = Rename.label("Movie", "'Whatever\\u0060 WITH s MATCH (m) DETACH DELETE m //'");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatAllLabelsHaveBeenRenamed(session, counters, "'Whatever\u0060 WITH s MATCH (m) DETACH DELETE m //'");
		}
	}

	@Test
	@EnabledIf("customQueriesSupported")
	void shouldSafelyRenamePropertiesWithMultipleCalls() {

		Rename rename = Rename.nodeProperty("released", "veröffentlicht im Jahr")
			.withCustomQuery("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);

			Counters counters = rename.apply(refactoringContext);
			assertThat(counters.propertiesSet()).isEqualTo(6);

			counters = rename.apply(refactoringContext);
			assertThat(counters.propertiesSet()).isZero();

			List<String> properties = session.run(""
				+ "MATCH (n:Movie)\n"
				+ "WITH keys(n) as properties\n"
				+ "UNWIND properties as property\n"
				+ "RETURN distinct property ORDER BY property"
			).list(r -> r.get("property").asString());
			assertThat(properties).containsExactly("released", "tagline", "title", "veröffentlicht im Jahr");
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldRenameLabelsInBatches() {

		Rename rename = Rename.label("Movie", "Film").inBatchesOf(23);

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatAllLabelsHaveBeenRenamed(session, counters);
		}
	}

	private static void assertThatAllLabelsHaveBeenRenamed(Session session, Counters counters) {

		assertThatAllLabelsHaveBeenRenamed(session, counters, "Film");
	}

	private static void assertThatAllLabelsHaveBeenRenamed(Session session, Counters counters, String newName) {
		assertThat(counters.nodesCreated()).isZero();
		assertThat(counters.nodesDeleted()).isZero();
		assertThat(counters.labelsAdded()).isEqualTo(38);
		assertThat(counters.labelsRemoved()).isEqualTo(38);
		assertThat(counters.typesAdded()).isZero();
		assertThat(counters.typesRemoved()).isZero();

		List<String> labels = session.run(""
			+ "MATCH (n) UNWIND labels(n) AS label\n"
			+ "WITH label ORDER BY label ASC\n"
			+ "RETURN collect(distinct label) AS labels"
		).single().get("labels").asList(Value::asString);
		assertThat(labels).containsExactlyInAnyOrder("Person", newName);
	}

	@Test
	@EnabledIf("customQueriesSupported")
	void shouldRenameLabelsWithCustomQuery() {

		Rename rename = Rename.label("Movie", "Film")
			.withCustomQuery("MATCH (m:Movie) WHERE m.title =~ '.*Matrix.*' RETURN m");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatSomeLabelsHaveBeenRenamed(session, counters);
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldRenameLabelsWithCustomQueryInBatches() {

		Rename rename = Rename.label("Movie", "Film")
			.withCustomQuery("MATCH (m:Movie) WHERE m.title =~ '.*Matrix.*' RETURN m")
			.inBatchesOf(23);

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatSomeLabelsHaveBeenRenamed(session, counters);
		}
	}

	private static void assertThatSomeLabelsHaveBeenRenamed(Session session, Counters counters) {
		assertThat(counters.nodesCreated()).isZero();
		assertThat(counters.nodesDeleted()).isZero();
		assertThat(counters.labelsAdded()).isEqualTo(3);
		assertThat(counters.labelsRemoved()).isEqualTo(3);
		assertThat(counters.typesAdded()).isZero();
		assertThat(counters.typesRemoved()).isZero();

		List<String> labels = session.run(""
			+ "MATCH (n) UNWIND labels(n) AS label\n"
			+ "WITH label ORDER BY label ASC\n"
			+ "RETURN collect(distinct label) AS labels"
		).single().get("labels").asList(Value::asString);
		assertThat(labels).containsExactly("Film", "Movie", "Person");
	}

	@Test
	void shouldRenameTypes() {

		Rename rename = Rename.type("ACTED_IN", "HAT_GESPIELT_IN");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatAllRelationshipsHaveBeenRecreated(session, counters);
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldRenameTypesInBatches() {

		Rename rename = Rename.type("ACTED_IN", "HAT_GESPIELT_IN")
			.inBatchesOf(13);

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatAllRelationshipsHaveBeenRecreated(session, counters);
		}
	}

	private static void assertThatAllRelationshipsHaveBeenRecreated(Session session, Counters counters) {
		assertThat(counters.nodesCreated()).isZero();
		assertThat(counters.nodesDeleted()).isZero();
		assertThat(counters.labelsAdded()).isZero();
		assertThat(counters.labelsRemoved()).isZero();
		assertThat(counters.typesAdded()).isEqualTo(172);
		assertThat(counters.typesRemoved()).isEqualTo(172);

		List<String> labels = session.run("match ()-[r]-() with distinct type(r) as type order by type return collect(type)")
			.single().get(0).asList(Value::asString);
		assertThat(labels).containsExactly("DIRECTED", "FOLLOWS", "HAT_GESPIELT_IN", "PRODUCED", "REVIEWED", "WROTE");
	}

	@Test
	@EnabledIf("customQueriesSupported")
	void shouldRenameTypesWithCustomQuery() {

		Rename rename = Rename.type("ACTED_IN", "HAT_GESPIELT_IN")
			.withCustomQuery("MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n");

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatSomeRelationshipsHaveBeenRecreated(session, counters);
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldRenameTypesWithCustomQueryWithCustomQueryInBatches() {

		Rename rename = Rename.type("ACTED_IN", "HAT_GESPIELT_IN")
			.withCustomQuery("MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n")
			.inBatchesOf(1);

		try (Session session = driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(driver::session);
			Counters counters = rename.apply(refactoringContext);

			assertThatSomeRelationshipsHaveBeenRecreated(session, counters);
		}
	}

	private static void assertThatSomeRelationshipsHaveBeenRecreated(Session session, Counters counters) {
		assertThat(counters.nodesCreated()).isZero();
		assertThat(counters.nodesDeleted()).isZero();
		assertThat(counters.labelsAdded()).isZero();
		assertThat(counters.labelsRemoved()).isZero();
		assertThat(counters.typesAdded()).isEqualTo(13);
		assertThat(counters.typesRemoved()).isEqualTo(13);

		List<String> labels = session.run("match ()-[r]-() with distinct type(r) as type order by type return collect(type)")
			.single().get(0).asList(Value::asString);
		assertThat(labels).containsExactly("ACTED_IN", "DIRECTED", "FOLLOWS", "HAT_GESPIELT_IN", "PRODUCED", "REVIEWED", "WROTE");
	}
}
