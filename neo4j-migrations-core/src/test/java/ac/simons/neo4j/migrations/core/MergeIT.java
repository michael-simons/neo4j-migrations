/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.internal.Strings;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Merge;
import ac.simons.neo4j.migrations.core.refactorings.Merge.PropertyMergePolicy;
import ac.simons.neo4j.migrations.core.refactorings.Merge.PropertyMergePolicy.Strategy;
import ac.simons.neo4j.migrations.core.refactorings.QueryRunner;
import ac.simons.neo4j.migrations.core.refactorings.RefactoringContext;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MergeIT extends AbstractRefactoringsITTestBase {

	static Stream<Arguments> shouldMergePropertiesAccordingToTheRespectivePolicy() {
		return Stream.of(
				Arguments.of(PropertyMergePolicy.of("name", Strategy.KEEP_ALL), Arrays.asList("Anastasia", "Zouheir")),
				Arguments.of(PropertyMergePolicy.of("name", Strategy.KEEP_FIRST),
						Collections.singletonList("Anastasia")),
				Arguments.of(PropertyMergePolicy.of("name", Strategy.KEEP_LAST), Collections.singletonList("Zouheir")),
				Arguments.of(PropertyMergePolicy.of(".*", Strategy.KEEP_ALL), Arrays.asList("Anastasia", "Zouheir")),
				Arguments.of(PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST), Collections.singletonList("Anastasia")),
				Arguments.of(PropertyMergePolicy.of(".*", Strategy.KEEP_LAST), Collections.singletonList("Zouheir")));
	}

	@BeforeEach
	void clearDatabase() {

		try (Session session = this.driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
		}
	}

	@ParameterizedTest
	@CsvSource(nullValues = "n/a", delimiterString = "|", textBlock = """
			                      | MATCH (n) RETURN n
			CREATE (:Foo)         | MATCH (n:Bar) RETURN n
			CREATE (:Foo)         | MATCH (n) RETURN n
			CREATE (:Foo)         | MATCH (n:Foo) RETURN n
			CREATE (:Foo), (:Bar) | MATCH (n:Foo) RETURN n
			CREATE (:Foo), (:Bar) | MATCH (n:Bar) RETURN n
			""")
	void shouldNotGenerateAnyStatementsWhenLessThan2NodesMatch(String initQuery, String selectionQuery) {

		Merge merge = Merge.nodes(selectionQuery.trim());

		try (Session session = this.driver.session()) {
			Neo4jVersion version = Neo4jVersion.of(session
				.run("CALL dbms.components() YIELD name, versions WHERE name = 'Neo4j Kernel' RETURN versions[0]")
				.single()
				.get(0)
				.asString());

			List<String> queries = new ArrayList<>();
			if (!(initQuery == null || initQuery.trim().isEmpty())) {
				session.run(initQuery.trim()).consume();
			}

			RefactoringContext refactoringContext = new RefactoringContext() {
				@Override
				public Optional<String> findSingleResultIdentifier(String query) {
					return Optional.of("n");
				}

				@Override
				public QueryRunner getQueryRunner(QueryRunner.FeatureSet featureSet) {
					return query -> {
						queries.add(query.text());
						return MergeIT.this.driver.session()
							.run((version.getMajorVersion() < 5)
									? query.withText(Strings.replaceElementIdCalls(query.text())) : query);
					};
				}
			};

			Counters counters = merge.apply(refactoringContext);
			assertThat(counters).isSameAs(Counters.empty());
			assertThat(queries)
				.containsExactly("CALL { " + selectionQuery + " } WITH n RETURN collect(elementId(n)) AS ids");
		}
	}

	@Test
	void shouldMergeDisconnectedNodes() {

		Merge merge = Merge.nodes("MATCH (p) RETURN p");

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);

			session.run("CREATE (:Label1:Label3), (:Label2:`Label Oops`), (:Label1:`Label Oops`)").consume();

			Counters counters = merge.apply(refactoringContext);

			assertThat(counters.nodesCreated()).isZero();
			assertThat(counters.nodesDeleted()).isEqualTo(2);
			assertThat(counters.labelsAdded()).isEqualTo(2);
			assertThat(counters.labelsRemoved()).isZero();
			assertThat(counters.typesAdded()).isZero();
			assertThat(counters.typesRemoved()).isZero();
			List<String> labels = session.run("""
					MATCH (n) UNWIND labels(n) AS label
					WITH label ORDER BY label ASC
					RETURN collect(label) AS labels""").single().get("labels").asList(Value::asString);
			assertThat(labels).containsExactly("Label Oops", "Label1", "Label2", "Label3");
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldMergePropertiesAccordingToTheRespectivePolicy(PropertyMergePolicy policy, List<String> result) {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Collections.singletonList(policy));

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);

			session.run(
					"CREATE (:Person), (:Person {name: 'Anastasia'}), (:Unmatched {name: 'X'}), (:Person {name: 'Zouheir'}), (:Person)")
				.consume();

			merge.apply(refactoringContext);

			Record properties = session.run("MATCH (n:Person) RETURN n {.*}").single();
			if (policy.strategy() != Strategy.KEEP_ALL) {
				assertThat(properties.get("n").get("name").asString()).isEqualTo(result.get(0));
			}
			else {
				assertThat(properties.get("n").get("name").asList(Value::asString)).containsExactlyElementsOf(result);
			}
		}
	}

	@Test
	void singlePropertiesShouldBeMergedIntoArraysToo() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC",
				Collections.singletonList(PropertyMergePolicy.of(".*", Strategy.KEEP_ALL)));

		try (Session session = this.driver.session()) {
			session.run(
					"CREATE (:Person), (:Person {name: 'Anastasia', a:'b'}), (:Unmatched {name: 'X'}), (:Person {name: 'Zouheir'}), (:Person)")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			merge.apply(refactoringContext);

			Record properties = session.run("MATCH (n:Person) RETURN n {.*}").single();
			assertThat(properties.get("n").get("a").asList(Value::asString)).containsExactly("b");
		}
	}

	@Test
	void shouldFailToGenerateStatementsIfMergeablePropertiesDoNotHaveAPolicy() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Collections.emptyList());

		try (Session session = this.driver.session()) {
			session.run("CREATE (:Person {name: 'Anastasia'}), (:Person {name: 'Zouheir'})").consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			assertThatIllegalStateException().isThrownBy(() -> merge.apply(refactoringContext))
				.withMessage("Could not find merge policy for node property `name`");
		}
	}

	@Test
	void shouldGenerateStatementsForMergingIncomingRelationships() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
				PropertyMergePolicy.of("name", Strategy.KEEP_LAST), PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST)));

		try (Session session = this.driver.session()) {
			session.run(
					"CREATE (:Person {name: 'Anastasia', age: 22})<-[:MAINTAINED_BY]-(:Project {name: 'Secret'}), (:Person {name: 'Zouheir'})<-[:FOUNDED_BY {year: 2012}]-(:Conference {name: 'Devoxx France'})")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = merge.apply(refactoringContext);

			assertThat(counters.nodesDeleted()).isEqualTo(1);
			assertThat(counters.labelsAdded()).isZero();
			assertThat(counters.typesAdded()).isEqualTo(1);
			assertThat(counters.typesRemoved()).isEqualTo(1);
			Record row = session.run("""
					MATCH (p:Person)
					WITH p, [ (p)<-[incoming]-() | incoming ] AS allIncoming
					UNWIND allIncoming AS incoming
					WITH p, incoming
					ORDER BY type(incoming) ASC
					RETURN p, collect(incoming) AS allIncoming""").single();
			assertThat(row.get("p").get("name").asString()).isEqualTo("Zouheir");
			assertThat(row.get("p").get("age").asInt()).isEqualTo(22);
			assertThat(row.get("allIncoming").asList(Value::asRelationship)).satisfies(relationship -> {
				assertThat(relationship.type()).isEqualTo("FOUNDED_BY");
				assertThat(relationship.get("year").asInt()).isEqualTo(2012);
			}, Index.atIndex(0))
				.satisfies(relationship -> assertThat(relationship.type()).isEqualTo("MAINTAINED_BY"),
						Index.atIndex(1));
		}
	}

	@Test
	void shouldEscapeTypes() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
				PropertyMergePolicy.of("name", Strategy.KEEP_LAST), PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST)));

		try (Session session = this.driver.session()) {
			session.run(
					"CREATE (:Person {name: 'Anastasia', age: 42})<-[:MAINTAINED_BY]-(:Project {name: 'Secret'}), (:Conference {name: 'JavaLand'}) <-[:`HAT BESUCHT`]- (m:Person {name: 'Michael'})<-[:`FOUNDED BY` {year: 2015}]-(:JUG {name: 'EuregJUG'}), (m)-[:`DAS IST` {offensichtlich: true}]->(m)")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = merge.apply(refactoringContext);

			assertThat(counters.nodesDeleted()).isEqualTo(1);
			assertThat(counters.labelsAdded()).isZero();
			assertThat(counters.typesAdded()).isEqualTo(3);
			assertThat(counters.typesRemoved()).isEqualTo(3);

			Record row = session.run("""
					MATCH (p:Person)
					WITH p, [ (p)-[rel]-() | rel ] AS rels
					UNWIND rels AS rel
					WITH p, rel
					ORDER BY type(rel) ASC
					RETURN p, collect(rel) AS rels""").single();
			assertThat(row.get("p").get("name").asString()).isEqualTo("Michael");
			assertThat(row.get("p").get("age").asInt()).isEqualTo(42);
			assertThat(row.get("rels").asList(Value::asRelationship)).hasSize(4)
				.satisfies(relationship -> assertThat(relationship.type()).isEqualTo("DAS IST"), Index.atIndex(0))
				.satisfies(relationship -> {
					assertThat(relationship.type()).isEqualTo("FOUNDED BY");
					assertThat(relationship.get("year").asInt()).isEqualTo(2015);
				}, Index.atIndex(1))
				.satisfies(relationship -> assertThat(relationship.type()).isEqualTo("HAT BESUCHT"), Index.atIndex(2))
				.satisfies(relationship -> assertThat(relationship.type()).isEqualTo("MAINTAINED_BY"),
						Index.atIndex(3));
		}
	}

	@Test
	void shouldGenerateStatementsForMergingOutgoingRelationships() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
				PropertyMergePolicy.of("name", Strategy.KEEP_LAST), PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST)));

		try (Session session = this.driver.session()) {
			session.run(
					"CREATE (:Person {name: 'Anastasia', age: 22})-[:MAINTAINS]->(:Project {name: 'Secret'}), (:Person {name: 'Zouheir'})-[:FOUNDED {year: 2012}]->(:Conference {name: 'Devoxx France'})")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			merge.apply(refactoringContext);

			Record row = session.run("""
					MATCH (p:Person)
					WITH p, [ (p)-[outgoing]->() | outgoing ] AS allOutgoing
					UNWIND allOutgoing AS outgoing
					WITH p, outgoing
					ORDER BY type(outgoing) ASC
					RETURN p, collect(outgoing) AS allOutgoing""").single();
			assertThat(row.get("p").get("name").asString()).isEqualTo("Zouheir");
			assertThat(row.get("p").get("age").asInt()).isEqualTo(22);
			assertThat(row.get("allOutgoing").asList(Value::asRelationship)).satisfies(relationship -> {
				assertThat(relationship.type()).isEqualTo("FOUNDED");
				assertThat(relationship.get("year").asInt()).isEqualTo(2012);
			}, Index.atIndex(0))
				.satisfies(relationship -> assertThat(relationship.type()).isEqualTo("MAINTAINS"), Index.atIndex(1));
		}
	}

	@Test
	void shouldGenerateStatementsThatPreserveExistingSelfRelationships() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
				PropertyMergePolicy.of("name", Strategy.KEEP_LAST), PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST)));

		try (Session session = this.driver.session()) {
			session.run(
					"CREATE (anastasia:Person {name: 'Anastasia', age: 22})-[:IS {obviously: true}]->(anastasia), (zouheir:Person {name: 'Zouheir'})-[:IS_SAME_AS {evidently: true}]->(zouheir)")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			merge.apply(refactoringContext);

			Record row = session.run("""
					MATCH (p:Person)-[r]->(p)
					WITH r\s
					ORDER BY type(r) ASC
					RETURN collect(r) AS rels""").single();
			assertThat(row.get("rels").asList(Value::asRelationship)).satisfies(relationship -> {
				assertThat(relationship.type()).isEqualTo("IS");
				assertThat(relationship.get("obviously").asBoolean()).isTrue();
			}, Index.atIndex(0)).satisfies(relationship -> {
				assertThat(relationship.type()).isEqualTo("IS_SAME_AS");
				assertThat(relationship.get("evidently").asBoolean()).isTrue();
			}, Index.atIndex(1));
		}
	}

	@Test
	void shouldGenerateStatementsThatYieldsSelfRelationships() {

		Merge merge = Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
				PropertyMergePolicy.of("name", Strategy.KEEP_LAST), PropertyMergePolicy.of(".*", Strategy.KEEP_FIRST)));

		try (Session session = this.driver.session()) {
			session.run(""
					+ "CREATE (anastasia:Person {name: 'Anastasia', age: 22})-[:FOLLOWS_1 {direction: 'a to z'}]->(zouheir:Person {name: 'Zouheir'}), "
					+ "(zouheir)-[:FOLLOWS_2 {direction: 'z to a'}]->(anastasia),"
					+ "(zouheir)-[:FOLLOWS_3 {direction: 'z to z'}]->(zouheir),"
					+ "(zouheir)-[:FOLLOWS_4 {direction: 'z to m'}]->(:Person {name: 'Marouane'})")
				.consume();

			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			merge.apply(refactoringContext);

			Record row = session.run("""
					MATCH (p:Person)-[r]->(p)
					WITH r
					ORDER BY type(r) ASC
					RETURN collect(r) AS rels""").single();
			assertThat(row.get("rels").asList(Value::asRelationship))
				.map(r -> r.type() + " " + r.get("direction").asString())
				.containsExactly("FOLLOWS_1 a to z", "FOLLOWS_2 z to a", "FOLLOWS_3 z to z", "FOLLOWS_4 z to m");
		}
	}

}
