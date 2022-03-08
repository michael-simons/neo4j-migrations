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

import java.util.List;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class CypherResourceTest {

	@ParameterizedTest
	@ValueSource(strings = { "multiline_no_comments", "singleline_no_comments" })
	void shouldGetAllStatements(String r) {
		CypherResource cypherResource = new CypherResource(
			CypherResourceTest.class.getResource("/parsing/" + r + ".cypher"), false);

		assertThat(cypherResource.getStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments()).isEmpty();
		assertThat(cypherResource.getPreconditions()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "singleline_comments", "singleline_comments_no_nl", "multiline_comments",
		"comments_following_each_other1", "comments_following_each_other2" })
	void shouldGetAllStatementsAndComments(String r) {
		CypherResource cypherResource = new CypherResource(
			CypherResourceTest.class.getResource("/parsing/" + r + ".cypher"), false);

		assertThat(cypherResource.getStatements()).hasSize(4);
		assertThat(cypherResource.getExecutableStatements()).hasSize(3);
		assertThat(cypherResource.getSingleLineComments())
			.containsExactly(
				"// Right at the start",
				"// in the middle",
				"//the end"
			);
		assertThat(cypherResource.getPreconditions()).isEmpty();
	}

	@Test
	void singleLineFollowingEachOther() {
		CypherResource cypherResource = new CypherResource(
			CypherResourceTest.class.getResource("/preconditions/44/V0001__Create_existence_constraint.cypher"), false);
		assertThat(cypherResource.getStatements()).hasSize(1);
		assertThat(cypherResource.getSingleLineComments()).hasSize(3);
		List<Precondition> preconditions = cypherResource.getPreconditions();
		assertThat(preconditions).hasSize(3);
	}

	@Test
	void shouldRetrievePreconditions() {

		CypherResource cypherResource = new CypherResource(
			CypherResourceTest.class.getResource("/parsing/several_preconditions.cypher"), false);
		List<Precondition> preconditions = cypherResource.getPreconditions();
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
}
