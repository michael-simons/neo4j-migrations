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
package ac.simons.neo4j.migrations.core;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import ac.simons.neo4j.migrations.test_resources.TestResources;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
class DefaultCypherResourceTests {

	@ParameterizedTest
	@ValueSource(strings = { "multiline_no_comments", "singleline_no_comments" })
	void shouldGetAllStatements(String r) {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource
			.of(ResourceContext.of(DefaultCypherResourceTests.class.getResource("/parsing/" + r + ".cypher")));

		assertThat(cypherResource.getStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments()).isEmpty();
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "singleline_comments", "singleline_comments_no_nl", "multiline_comments",
			"comments_following_each_other1", "comments_following_each_other2" })
	void shouldGetAllStatementsAndComments(String r) {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource
			.of(ResourceContext.of(DefaultCypherResourceTests.class.getResource("/parsing/" + r + ".cypher")));

		assertThat(cypherResource.getStatements()).hasSize(4);
		assertThat(cypherResource.getExecutableStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments()).containsExactly("// Right at the start", "// in the middle",
				"//the end");
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).isEmpty();
	}

	@Test
	void onlyCommentsShouldBeTreatedAsSuch() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource
			.of(ResourceContext.of(DefaultCypherResourceTests.class.getResource("/parsing/onlycomments.cypher")));

		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getExecutableStatements()).isEmpty();
		assertThat(cypherResource.getSingleLineComments()).containsExactly("// Line 1", "// Line 2");
	}

	@Test
	void onlyCommentsShouldBeTreatedAsSuch2() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext
			.of(DefaultCypherResourceTests.class.getResource("/parsing/multiple_comments_one_command.cypher")));

		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getExecutableStatements()).containsExactly("""
				// Line 1
				// Line 2
				RETURN TRUE""");
		assertThat(cypherResource.getSingleLineComments()).containsExactly("// Line 1", "// Line 2");
	}

	@Test
	void singleLineFollowingEachOther() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext
			.of(TestResources.class.getResource("/preconditions/44/V0001__Create_existence_constraint.cypher")));
		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getSingleLineComments()).hasSize(3);
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).hasSize(3);
	}

	@Test
	void shouldRetrievePreconditions() {

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext
			.of(DefaultCypherResourceTests.class.getResource("/parsing/several_preconditions.cypher")));
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).hasSize(3).satisfies(precondition -> {
			assertThat(precondition).isInstanceOf(VersionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSUMPTION);
		}, Index.atIndex(0)).satisfies(precondition -> {
			assertThat(precondition).isInstanceOf(EditionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSERTION);
		}, Index.atIndex(1)).satisfies(precondition -> {
			assertThat(precondition).isInstanceOf(QueryPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSERTION);
			assertThat(((QueryPrecondition) precondition).getQuery()).isEqualTo("match (n:`007`) return count(n) = 0");
		}, Index.atIndex(2));
		assertThat(cypherResource.getSingleLineComments()).hasSize(5);
	}

	@ParameterizedTest // GH-232
	@ValueSource(strings = { "unix", "dos", "mac" })
	void unixDosOrMacLineEndingsMustNotChangeChecksum(String type) {

		URL resource = DefaultCypherResourceTests.class.getResource("/V0001__" + type + ".cypher");

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext.of(resource));
		List<String> statements = cypherResource.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n) RETURN count(n) AS n");
		assertThat(cypherResource.getChecksum()).isEqualTo("1902097523");
	}

	@ParameterizedTest // GH-238
	@ValueSource(strings = { "unix", "dos" })
	void shouldConvertLineEndings(String type) {

		URL resource = TestResources.class.getResource("/ml/" + type + "/V0001__Just a couple of matches.cypher");

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource
			.of(ResourceContext.of(resource, MigrationsConfig.builder().withAutocrlf(true).build()));
		List<String> statements = cypherResource.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n)\nRETURN count(n) AS n");
		assertThat(cypherResource.getChecksum()).isEqualTo("1916418554");
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			true,-626195011
			false,1491717096
			""")
	void shouldConvertLineEndings(boolean enabled, String expected) {
		URL resource = TestResources.class.getResource("/my/awesome/migrations/V5000__WithCommentAtEnd.cypher");
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext.of(resource,
				MigrationsConfig.builder().withFlywayCompatibleChecksums(enabled).build()));
		assertThat(cypherResource.getChecksum()).isEqualTo(expected);
	}

	@Test
	void shouldBeAbleToReadFromJar() {

		// Those are in eu.michael-simons.neo4j.neo4j-migrations:test-migrations
		URL resource = TestResources.class.getResource("/some/changeset/V0002__create_new_data.cypher");
		DefaultCypherResource migration = (DefaultCypherResource) CypherResource.of(ResourceContext.of(resource));

		List<String> statements = migration.getStatements();
		assertThat(statements).containsExactly("CREATE (n:FixedData) RETURN n", "MATCH (n) RETURN count(n) AS foobar");
	}

	@Test
	void shouldHandleMultilineStatements() {

		URL resource = TestResources.class
			.getResource("/my/awesome/migrations/moreStuff/V007__BondTheNameIsBond.cypher");
		DefaultCypherResource migration = (DefaultCypherResource) CypherResource.of(ResourceContext.of(resource));
		List<String> statements = migration.getStatements();
		assertThat(statements).hasSize(2);
	}

	@Test
	void shouldComputeCheckSum() {

		URL resource = TestResources.class.getResource("/some/changeset/V0001__delete_old_data.cypher");

		CypherBasedMigration migration = new CypherBasedMigration(ResourceContext.of(resource));

		assertThat(migration.getChecksum()).hasValue("1100083332");
	}

	@Test
	void alternateChecksumsShouldContainChecksumWithoutPrecondition() {

		URL script1 = DefaultCypherResourceTests.class.getResource("/parsing/V01__without_assumption.cypher");

		DefaultCypherResource cypherResource1 = (DefaultCypherResource) CypherResource.of(ResourceContext.of(script1));
		assertThat(cypherResource1.getChecksum()).isEqualTo("1995107586");

		CypherBasedMigration migrationWithoutAssumptions = new CypherBasedMigration(ResourceContext.of(script1));
		assertThat(migrationWithoutAssumptions.getChecksum()).hasValue("1995107586");
		assertThat(migrationWithoutAssumptions.getAlternativeChecksums()).isEmpty();

		URL script2 = DefaultCypherResourceTests.class.getResource("/parsing/V01__with_assumption.cypher");

		DefaultCypherResource cypherResource2 = (DefaultCypherResource) CypherResource.of(ResourceContext.of(script2));
		assertThat(cypherResource2.getChecksum()).isEqualTo("2044432884");

		CypherBasedMigration migrationWithAssumptions = new CypherBasedMigration(ResourceContext.of(script2));
		assertThat(migrationWithAssumptions.getChecksum()).hasValue("2044432884");
		assertThat(migrationWithAssumptions.getAlternativeChecksums()).contains("1995107586");
	}

	@SuppressWarnings("deprecation")
	@Test
	void shouldParseDescription() {

		URL url = DefaultCypherResourceTests.class.getResource("/parsing/V01__without_assumption.cypher");

		Migration migration = new CypherBasedMigration(ResourceContext.of(url));
		assertThat(migration.getOptionalDescription()).hasValue("without assumption");
	}

	@Test
	void randomCommentsMustChangeChecksums() {

		URL script1 = DefaultCypherResourceTests.class.getResource("/parsing/V01__with_random_comments.cypher");

		DefaultCypherResource cypherResource1 = (DefaultCypherResource) CypherResource.of(ResourceContext.of(script1));
		assertThat(cypherResource1.getChecksum()).isNotEqualTo("2104077345");

		CypherBasedMigration migrationWithoutAssumptions = new CypherBasedMigration(ResourceContext.of(script1));
		assertThat(migrationWithoutAssumptions.getChecksum())
			.hasValueSatisfying(v -> assertThat(v).isNotEqualTo("2104077345"));
		assertThat(migrationWithoutAssumptions.getAlternativeChecksums()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			:use abc,abc
			:use ABC,abc
			:use Abc,abc
			:use abc,abc
			:Use whatever1-will-be.Que-sera,whatever1-will-be.que-sera
			:use  hallo,hallo
			:use the-force;,the-force
			""")
	void validDatabaseNamesShouldBeRecognized(String line, String expected) {
		Optional<String> databaseName = DefaultCypherResource.getDatabaseName(line);
		assertThat(databaseName).hasValue(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { ":use umbruch-umbruch\n", ":use umbruch-umbruch;\n" })
	void validDatabaseNamesShouldBeRecognized(String line) {
		Optional<String> databaseName = DefaultCypherResource.getDatabaseName(line);
		assertThat(databaseName).hasValue("umbruch-umbruch");
	}

	@ParameterizedTest
	@ValueSource(strings = { "1ab", "ab", "aab__b" })
	void invalidDbNames(String line) {
		Optional<String> databaseName = DefaultCypherResource.getDatabaseName(":use " + line);
		assertThat(databaseName).isEmpty();
	}

	@Test
	void shouldHandleUseStatements() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext
			.of(DefaultCypherResourceTests.class.getResource("/parsing/with_use_statements.cypher")));

		assertThat(cypherResource.getExecutableStatements()).containsExactly("MATCH (n) RETURN count(n)",
				"CREATE (f) RETURN f", ":use system",
				"CALL apoc.trigger.add('test', \"CALL apoc.log.info('OK') RETURN 1\", { phase: 'after' })",
				":use neo4j", "MATCH (n)\nDETACH DELETE n", ":use system", "CALL apoc.trigger.remove('testTrigger')");
		assertThat(cypherResource.getSingleLineComments()).isEmpty();

		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).isEmpty();

		List<DefaultCypherResource.DatabaseAndStatements> groups = DefaultCypherResource
			.groupStatements(cypherResource.getExecutableStatements());
		assertThat(groups).hasSize(4).satisfies(e -> {
			assertThat(e.database()).isEmpty();
			assertThat(e.statements()).containsExactly("MATCH (n) RETURN count(n)", "CREATE (f) RETURN f");
		}, Index.atIndex(0)).satisfies(e -> {
			assertThat(e.database()).hasValue("system");
			assertThat(e.statements()).containsExactly(
					"CALL apoc.trigger.add('test', \"CALL apoc.log.info('OK') RETURN 1\", { phase: 'after' })");
		}, Index.atIndex(1)).satisfies(e -> {
			assertThat(e.database()).hasValue("neo4j");
			assertThat(e.statements()).containsExactly("MATCH (n)\nDETACH DELETE n");
		}, Index.atIndex(2)).satisfies(e -> {
			assertThat(e.database()).hasValue("system");
			assertThat(e.statements()).containsExactly("CALL apoc.trigger.remove('testTrigger')");
		}, Index.atIndex(3));
	}

	@Test
	void shouldDetectWrongUseStatements() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(ResourceContext
			.of(DefaultCypherResourceTests.class.getResource("/parsing/with_use_statements_wrong.cypher")));

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(cypherResource::getExecutableStatements)
			.withMessage("""
					Can't switch database inside a statement, offending statement:
					MATCH (n)
					:use something
					DETACH DELETE n""");
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			USING PERIODIC COMMIT 500 LOAD CSV FROM 'file:///artists.csv' AS line
			CREATE (:Artist {name: line[1], year: toInteger(line[2])})
			""",
			"   USING PERIODIC COMMIT 500 LOAD CSV FROM 'file:///artists.csv' AS line CREATE (:Artist {name: line[1], year: toInteger(line[2])})",
			"""
					LOAD CSV FROM 'file:///friends.csv' AS line
					CALL {
					  WITH line
					  CREATE (:PERSON {name: line[1], age: toInteger(line[2])})
					} IN TRANSACTIONS
					""", """
					WITH `n` as line CALL {
					  WITH line
					  CREATE (:`PERSON` {name: line[1], age: toInteger(line[2])})
					} IN TRANSACTIONS
					""", "MATCH(n)CALL(n){MATCH (m)-->(n)RETURN m}IN TRANSACTIONS RETURN m", """
					MATCH (n) \
					CALL(n) {
					  MATCH (m)-->(n)
					  RETURN m
					}  IN TRANSACTIONS RETURN m
					""", "MATCH (n) CALL(n) {MATCH (m)-->(n)RETURN m}  IN TRANSACTIONS RETURN y",
			"MATCH(n)CALL(n){MATCH (m)-->(n)RETURN m}IN -2 CONCURRENT TRANSACTIONS OF 23 ROWS RETURN x",
			"MATCH(n)CALL(n){MATCH (m)-->(n)RETURN m}IN-1 CONCURRENT TRANSACTIONS OF 23 ROWS RETURN m",
			"MATCH(n)CALL(n){MATCH (m)-->(n)RETURN m}IN 42 CONCURRENT TRANSACTIONS OF 23 ROWS RETURN m",
			"MATCH (n) CALL {RETURN 1 AS x} IN concurrent TRANSACTIONS RETURN *" })
	void shouldDetectImplicitTransactionNeeds(String query) {

		assertThat(DefaultCypherResource.getTransactionMode(query))
			.isEqualTo(DefaultCypherResource.TransactionMode.IMPLICIT);
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			UNWIND [0, 1, 2] AS x
			CALL {
			  WITH x
			  RETURN x * 10 AS y
			}
			RETURN x, y
			""", "CREATE (a:`USING PERIODIC COMMIT `) RETURN a", "CREATE (a:`USING PERIODIC COMMIT `) RETURN a",
			"CREATE (a:`CALL {WITH WHATEVER} IN TRANSACTIONS`) RETURN a",
			"CREATE (a: `  CALL {WITH WHATEVER} IN TRANSACTIONS`) RETURN a",
			"MATCH (n) CALL {blub}concurrent IN TRANSACTIONS RETURN n", """
					WITH apoc.date.currentTimestamp() AS start
					CALL apoc.util.sleep(4000)
					WITH start, apoc.date.currentTimestamp() AS end
					RETURN datetime({epochmillis: start}) AS start, datetime({epochmillis: end}) AS end;
					""" })
	void shouldDetectManagedTransactionNeeds(String query) {

		assertThat(DefaultCypherResource.getTransactionMode(query))
			.isEqualTo(DefaultCypherResource.TransactionMode.MANAGED);
	}

	@ParameterizedTest
	@ValueSource(strings = { "MATCH (n) CALL {blub} IN concurrently TRANSACTIONS RETURN n" })
	void weEvenMightThrowWhenWeDoHalfwayParsingAnyway(String query) {

		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(() -> DefaultCypherResource.getTransactionMode(query));
	}

}
