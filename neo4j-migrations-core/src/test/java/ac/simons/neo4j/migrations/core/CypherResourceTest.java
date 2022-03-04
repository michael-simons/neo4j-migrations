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
	}
}
