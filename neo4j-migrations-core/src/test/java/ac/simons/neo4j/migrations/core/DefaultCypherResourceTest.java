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

import java.net.URL;
import java.util.List;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class DefaultCypherResourceTest {

	@ParameterizedTest
	@ValueSource(strings = { "multiline_no_comments", "singleline_no_comments" })
	void shouldGetAllStatements(String r) {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/parsing/" + r + ".cypher"));

		assertThat(cypherResource.getStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments()).isEmpty();
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "singleline_comments", "singleline_comments_no_nl", "multiline_comments",
		"comments_following_each_other1", "comments_following_each_other2" })
	void shouldGetAllStatementsAndComments(String r) {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/parsing/" + r + ".cypher"));

		assertThat(cypherResource.getStatements()).hasSize(4);
		assertThat(cypherResource.getExecutableStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments())
			.containsExactly(
				"// Right at the start",
				"// in the middle",
				"//the end"
			);
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).isEmpty();
	}

	@Test
	void onlyCommentsShouldBeTreatedAsSuch() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/parsing/onlycomments.cypher"));

		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getExecutableStatements()).isEmpty();
		assertThat(cypherResource.getSingleLineComments()).containsExactly("// Line 1", "// Line 2");
	}

	@Test
	void onlyCommentsShouldBeTreatedAsSuch2() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/parsing/multiple_comments_one_command.cypher"));

		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getExecutableStatements()).containsExactly(
			"// Line 1\n"
			+ "// Line 2\n"
			+ "RETURN TRUE"
		);
		assertThat(cypherResource.getSingleLineComments()).containsExactly("// Line 1", "// Line 2");
	}

	@Test
	void singleLineFollowingEachOther() {
		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/preconditions/44/V0001__Create_existence_constraint.cypher"));
		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getSingleLineComments()).hasSize(3);
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions).hasSize(3);
	}

	@Test
	void shouldRetrievePreconditions() {

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(
			DefaultCypherResourceTest.class.getResource("/parsing/several_preconditions.cypher"));
		List<Precondition> preconditions = Precondition.in(cypherResource);
		assertThat(preconditions)
			.hasSize(3)
			.satisfies(precondition -> {
				assertThat(precondition).isInstanceOf(VersionPrecondition.class);
				assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSUMPTION);
			}, Index.atIndex(0))
			.satisfies(precondition -> {
				assertThat(precondition).isInstanceOf(EditionPrecondition.class);
				assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSERTION);
			}, Index.atIndex(1))
			.satisfies(precondition -> {
				assertThat(precondition).isInstanceOf(QueryPrecondition.class);
				assertThat(precondition.getType()).isEqualTo(Precondition.Type.ASSERTION);
				assertThat(((QueryPrecondition) precondition).getQuery()).isEqualTo(
					"match (n:`007`) return count(n) = 0");
			}, Index.atIndex(2));
		assertThat(cypherResource.getSingleLineComments()).hasSize(5);
	}

	@ParameterizedTest // GH-232
	@ValueSource(strings = {"unix", "dos", "mac"})
	void unixDosOrMacLineEndingsMustNotChangeChecksum(String type)  {

		URL resource = DefaultCypherResourceTest.class.getResource("/V0001__" + type + ".cypher");

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(resource);
		List<String> statements = cypherResource.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n) RETURN count(n) AS n");
		assertThat(cypherResource.getChecksum()).isEqualTo("1902097523");
	}

	@ParameterizedTest // GH-238
	@ValueSource(strings = {"unix", "dos"})
	void shouldConvertLineEndings(String type)  {

		URL resource = DefaultCypherResourceTest.class.getResource("/ml/" + type + "/V0001__Just a couple of matches.cypher");

		DefaultCypherResource cypherResource = (DefaultCypherResource) CypherResource.of(resource, true);
		List<String> statements = cypherResource.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n)\nRETURN count(n) AS n");
		assertThat(cypherResource.getChecksum()).isEqualTo("1916418554");
	}

	@Test
	void shouldBeAbleToReadFromJar() {

		// Those are in eu.michael-simons.neo4j.neo4j-migrations:test-migrations
		URL resource = DefaultCypherResourceTest.class.getResource("/some/changeset/V0002__create_new_data.cypher");
		DefaultCypherResource migration = (DefaultCypherResource) CypherResource.of(resource);

		List<String> statements = migration.getStatements();
		assertThat(statements).containsExactly("CREATE (n:FixedData) RETURN n", "MATCH (n) RETURN count(n) AS foobar");
	}

	@Test
	void shouldHandleMultilineStatements() {

		URL resource = DefaultCypherResourceTest.class
			.getResource("/my/awesome/migrations/moreStuff/V007__BondTheNameIsBond.cypher");
		DefaultCypherResource migration = (DefaultCypherResource) CypherResource.of(resource);
		List<String> statements = migration.getStatements();
		assertThat(statements).hasSize(2);
	}

	@Test
	void shouldComputeCheckSum() {

		URL resource = DefaultCypherResourceTest.class.getResource("/some/changeset/V0001__delete_old_data.cypher");

		CypherBasedMigration migration = new CypherBasedMigration(resource);

		assertThat(migration.getChecksum()).hasValue("1100083332");
	}
}
