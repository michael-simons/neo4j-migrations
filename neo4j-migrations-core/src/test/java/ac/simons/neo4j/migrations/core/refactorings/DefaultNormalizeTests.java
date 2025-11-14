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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ac.simons.neo4j.migrations.core.Neo4jVersion;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class DefaultNormalizeTests {

	@Test
	void shouldGenerateStandardQuery() {

		String expected = """
				CALL { MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t } WITH t AS e
				WHERE e.`a property` IS NOT NULL
				SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END""";

		DefaultNormalize normalize = new DefaultNormalize("a property", Collections.emptyList(),
				Collections.emptyList());
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(query.parameters().get("falseValues")).isEqualTo(Values.value(Collections.emptyList()));
	}

	@Test
	void shouldBatch() {

		String expected = """
				CALL { MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t } WITH t AS e
				WHERE e.`a property` IS NOT NULL
				CALL { WITH e SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END } IN TRANSACTIONS OF 15 ROWS""";

		DefaultNormalize normalize = (DefaultNormalize) new DefaultNormalize("a property", Collections.emptyList(),
				Collections.emptyList())
			.inBatchesOf(15);
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(query.parameters().get("falseValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(normalize.getFeatures().requiredVersion()).isEqualTo("4.4");
	}

	@Test
	void shouldGenerateQueryWithCustomQuery() {

		String expected = """
				CALL { MATCH (n:`A Movie`) RETURN n } WITH n AS e
				WHERE e.`a property` IS NOT NULL
				SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END""";

		DefaultNormalize normalize = (DefaultNormalize) new DefaultNormalize("a property", Collections.emptyList(),
				Collections.emptyList())
			.withCustomQuery("MATCH (n:`A Movie`) RETURN n");
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(query.parameters().get("falseValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(normalize.getFeatures().requiredVersion()).isEqualTo("4.1");
	}

	@Test
	void shouldBatchAndUseCustomQuery() {

		String expected = """
				CALL { MATCH (n:`A Movie`) RETURN n } WITH n AS e
				WHERE e.`a property` IS NOT NULL
				CALL { WITH e SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END } IN TRANSACTIONS OF 15 ROWS""";

		DefaultNormalize normalize = (DefaultNormalize) new DefaultNormalize("a property", Collections.emptyList(),
				Collections.emptyList())
			.withCustomQuery("MATCH (n:`A Movie`) RETURN n")
			.inBatchesOf(15);
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(query.parameters().get("falseValues")).isEqualTo(Values.value(Collections.emptyList()));
		assertThat(query.parameters().get("nullValue").isNull()).isTrue();
	}

	@Test
	void shouldFillValues() {

		List<Object> trueValues = new ArrayList<>();
		trueValues.add("ja");
		trueValues.add("YES");
		trueValues.add(1L);

		List<Object> falseValues = new ArrayList<>();
		falseValues.add("ein pferd");
		falseValues.add("no");
		falseValues.add(0L);
		falseValues.add(Values.value("vielleicht"));
		falseValues.add(null);

		String expected = """
				CALL { MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t } WITH t AS e
				SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END""";

		DefaultNormalize normalize = new DefaultNormalize("a property", trueValues, falseValues);
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues").asList()).containsAll(trueValues);
		assertThat(query.parameters().get("falseValues").asList()).containsExactly("ein pferd", "no", 0L, "vielleicht");
		assertThat(query.parameters().get("nullValue").asBoolean()).isFalse();
	}

	@Test
	void shouldFilterOnNull() {

		List<Object> trueValues = new ArrayList<>();
		trueValues.add("ja");
		trueValues.add("YES");
		trueValues.add(1L);
		trueValues.add(Values.NULL);

		List<Object> falseValues = new ArrayList<>();

		String expected = """
				CALL { MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t } WITH t AS e
				SET e.`a property` = CASE
				  WHEN e.`a property` IN $trueValues THEN true
				  WHEN e.`a property` IN $falseValues THEN false
				  WHEN e.`a property` IN [true, false] THEN e.`a property`
				  ELSE $nullValue
				END""";

		DefaultNormalize normalize = new DefaultNormalize("a property", trueValues, falseValues);
		Query query = normalize.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));

		assertThat(query.text()).isEqualTo(expected);
		assertThat(query.parameters().get("trueValues").asList()).containsExactly("ja", "YES", 1L);
		assertThat(query.parameters().get("falseValues").asList()).isEmpty();
		assertThat(query.parameters().get("nullValue").asBoolean()).isTrue();
	}

	// Sonar, are you drunk?! Both collections are used
	@SuppressWarnings("squid:S4030")
	@Test
	void shouldThrowOnContradictingValues() {

		List<Object> trueValues = new ArrayList<>();
		trueValues.add("Y");

		List<Object> falseValues = new ArrayList<>();
		falseValues.add("Y");

		assertThatIllegalArgumentException().isThrownBy(() -> Normalize.asBoolean("a", trueValues, falseValues))
			.withMessage("Both true and false values contain `Y`");
	}

	// Sonar, are you drunk?! Both collections are used
	@SuppressWarnings("squid:S4030")
	@Test
	void shouldThrowOnContradictingValuesWithNulls() {
		List<Object> trueValues = new ArrayList<>();
		trueValues.add("not null");
		trueValues.add(null);

		List<Object> falseValues = new ArrayList<>();
		falseValues.add(null);

		assertThatIllegalArgumentException().isThrownBy(() -> Normalize.asBoolean("a", trueValues, falseValues))
			.withMessage("Both true and false values contain the literal value null");
	}

}
