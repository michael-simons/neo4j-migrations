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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultListToVectorTests {

	@Test
	void shouldFailOnEmptyLabel() {
		assertThatNullPointerException().isThrownBy(() -> ListToVector.onNodes(null))
			.withMessage("Primary label is required");
	}

	@Test
	void shouldFailOnEmptyType() {
		assertThatNullPointerException().isThrownBy(() -> ListToVector.onRelationships(null))
			.withMessage("Type is required");
	}

	@Test
	void shouldFailOnNullProperty() {
		var r = ListToVector.onRelationships("f");
		assertThatNullPointerException().isThrownBy(() -> r.withProperty(null))
			.withMessage("Property name is required");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "\t\n" })
	void shouldFailOnEmptyProperty(String name) {
		var r = ListToVector.onRelationships("f");
		assertThatIllegalArgumentException().isThrownBy(() -> r.withProperty(name))
			.withMessage("Property name must not be blank");
	}

	static Stream<Arguments> generateQueryShouldWorkWithIdentifiers() {
		var queryNodeWithoutBatch = "CYPHER 25 MATCH (n:$all($identifiers)) WHERE n[$property] :: LIST<FLOAT NOT NULL> NOT NULL SET n[$property] = VECTOR(n[$property], size(n[$property]), FLOAT)";
		var queryNodeWithBatch = "CYPHER 25 MATCH (n:$all($identifiers)) WHERE n[$property] :: LIST<FLOAT NOT NULL> NOT NULL CALL(n) { SET n[$property] = VECTOR(n[$property], size(n[$property]), FLOAT) } IN TRANSACTIONS OF $batchSize ROWS";
		var queryRelWithoutBatch = "CYPHER 25 MATCH ()-[r:$all($identifiers)]->() WHERE r[$property] :: LIST<FLOAT NOT NULL> NOT NULL SET r[$property] = VECTOR(r[$property], size(r[$property]), FLOAT)";
		var queryRelWithBatch = "CYPHER 25 MATCH ()-[r:$all($identifiers)]->() WHERE r[$property] :: LIST<FLOAT NOT NULL> NOT NULL CALL(r) { SET r[$property] = VECTOR(r[$property], size(r[$property]), FLOAT) } IN TRANSACTIONS OF $batchSize ROWS";
		return Stream.of(
				Arguments.of(queryNodeWithoutBatch, "embedding", null, DefaultListToVector.Target.NODE, "Le Test",
						null),
				Arguments.of(queryNodeWithBatch, "embedding", 23, DefaultListToVector.Target.NODE, "Le Test", null),
				Arguments.of(queryNodeWithoutBatch, "something else", null, DefaultListToVector.Target.NODE, "Le Test",
						new String[] { "Der Test", null, "Test" }),
				Arguments.of(queryRelWithoutBatch, "embedding", null, DefaultListToVector.Target.RELATIONSHIP,
						"Le Test", null),
				Arguments.of(queryRelWithBatch, "embedding", 23, DefaultListToVector.Target.RELATIONSHIP, "Le Test",
						null)

		);
	}

	@ParameterizedTest
	@MethodSource
	void generateQueryShouldWorkWithIdentifiers(String expected, String property, Integer batchSize,
			DefaultListToVector.Target target, String primaryLabel, String... additionalLabels) {
		ListToVector refactoring;
		if (target == DefaultListToVector.Target.NODE) {
			refactoring = ListToVector.onNodes(primaryLabel, additionalLabels);
		}
		else {
			refactoring = ListToVector.onRelationships(primaryLabel);
		}
		if (property != null) {
			refactoring = refactoring.withProperty(property);
		}
		if (batchSize != null) {
			refactoring = refactoring.inBatchesOf(batchSize);
		}
		var query = ((DefaultListToVector) refactoring).generateQuery(s -> Optional.of("n"));

		var expectedLabels = new ArrayList<String>();
		expectedLabels.add(primaryLabel);
		if (additionalLabels != null) {
			Collections.addAll(expectedLabels, additionalLabels);
			expectedLabels.removeIf(Objects::isNull);
		}

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("identifiers").asList(Value::asString))
			.containsExactlyElementsOf(expectedLabels);
		assertThat(query.parameters().get("property").asString()).isEqualTo(property);
		if (batchSize == null) {
			assertThat(query.parameters().containsKey("batchSize")).isFalse();
		}
		else {
			assertThat(query.parameters().get("batchSize").asInt()).isEqualTo(batchSize);
		}
	}

	static Stream<Arguments> generateQueryShouldWorkWithCustomQueries() {
		var queryNodeWithoutBatch = "CYPHER 25 CALL { MATCH (a) RETURN a } WITH a AS n SET n[$property] = VECTOR(n[$property], size(n[$property]), FLOAT)";
		var queryNodeWithBatch = "CYPHER 25 CALL { MATCH (a) RETURN a } WITH a AS n CALL(n) { SET n[$property] = VECTOR(n[$property], size(n[$property]), FLOAT) } IN TRANSACTIONS OF $batchSize ROWS";
		var queryRelWithoutBatch = "CYPHER 25 CALL { MATCH ()-[b]->() RETURN b } WITH b AS r SET r[$property] = VECTOR(r[$property], size(r[$property]), FLOAT)";
		var queryRelWithBatch = "CYPHER 25 CALL { MATCH ()-[b]->() RETURN b } WITH b AS r CALL(r) { SET r[$property] = VECTOR(r[$property], size(r[$property]), FLOAT) } IN TRANSACTIONS OF $batchSize ROWS";
		return Stream.of(true, false)
			.flatMap(v -> Stream.of(
					Arguments.of(v, queryNodeWithoutBatch, Optional.of("a"), "embedding", null,
							DefaultListToVector.Target.NODE, "MATCH (a) RETURN a"),
					Arguments.of(v, queryNodeWithBatch, Optional.of("a"), "embedding", 23,
							DefaultListToVector.Target.NODE, "MATCH (a) RETURN a"),
					Arguments.of(v, queryRelWithoutBatch, Optional.of("b"), "embedding", null,
							DefaultListToVector.Target.RELATIONSHIP, "MATCH ()-[b]->() RETURN b"),
					Arguments.of(v, queryRelWithBatch, Optional.of("b"), "embedding", 23,
							DefaultListToVector.Target.RELATIONSHIP, "MATCH ()-[b]->() RETURN b")));
	}

	@ParameterizedTest
	@MethodSource
	void generateQueryShouldWorkWithCustomQueries(boolean configureQueryLater, String expected,
			Optional<String> targetElement, String property, Integer batchSize, DefaultListToVector.Target target,
			String customQuery) {
		ListToVector refactoring;
		if (target == DefaultListToVector.Target.NODE) {
			refactoring = configureQueryLater ? ListToVector.onNodesMatching(customQuery)
					: ListToVector.onNodes("F").withCustomQuery(customQuery);

		}
		else {
			refactoring = configureQueryLater ? ListToVector.onRelationshipsMatching(customQuery)
					: ListToVector.onRelationships("F").withCustomQuery(customQuery);
		}
		if (property != null) {
			refactoring = refactoring.withProperty(property);
		}
		if (batchSize != null) {
			refactoring = refactoring.inBatchesOf(batchSize);
		}
		var query = ((DefaultListToVector) refactoring).generateQuery(s -> targetElement);

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().containsKey("identifiers")).isFalse();
		assertThat(query.parameters().get("property").asString()).isEqualTo(property);
		if (batchSize == null) {
			assertThat(query.parameters().containsKey("batchSize")).isFalse();
		}
		else {
			assertThat(query.parameters().get("batchSize").asInt()).isEqualTo(batchSize);
		}
	}

}
