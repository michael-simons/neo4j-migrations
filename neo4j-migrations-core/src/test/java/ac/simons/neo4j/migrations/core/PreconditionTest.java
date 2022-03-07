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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.EditionPrecondition.Edition;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
class PreconditionTest {

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that neo4j is 3.4; ASSUMPTION; 3.4",
		"// assUMe that neo4j is 3.4 or 3.5; ASSUMPTION; 3.4, 3.5",
		"// assume that neO4j is 3.4, 3.5, 4.4 or 5.0; ASSUMPTION; 3.4, 3.5, 4.4, 5.0",
		"// ASSERT that neo4j is 3.4, 3.5, 4.4 or 5.0; ASSERTION; 3.4, 3.5, 4.4, 5.0",
		"// assert that neo4j is 3.4,3.5, 4.4 or 5.0; ASSERTION; 3.4, 3.5, 4.4, 5.0",
		"// ASSERT that Neo4j is 3.4; ASSERTION; 3.4",
		"//assume that neo4j is 3.4; ASSUMPTION; 3.4",
		"//assert that neo4j is 3.4; ASSERTION; 3.4",
		"//assert that neo4j is 3 or 4; ASSERTION; 3, 4",
	})
	void shouldParseVersionPreconditions(String value, Precondition.Type expectedType, String expectedVersions) {

		Set<String> expectedVersions_ = Arrays.stream(expectedVersions.split(","))
			.map(String::trim)
			.map(s -> "Neo4j/" + s)
			.collect(Collectors.toCollection(TreeSet::new));

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(VersionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((VersionPrecondition) precondition).getVersions()).containsExactlyElementsOf(
				expectedVersions_);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that neo4j is something",
		"// assume that neo4j is ",
		"// assume that neo4j is",
	})
	void shouldFailOnWrongVersionPrecondition(String value) {
		assertThatIllegalArgumentException()
			.isThrownBy((() -> Precondition.parse(value)))
			.withMessage(
				"Wrong version precondition. Usage: `<assume|assert> that neo4j is <versions>`. With <versions> being a comma separated list of major.minor.patch versions.");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that edition is enterprise; ASSUMPTION; ENTERPRISE",
		"// assume that edition is EnteRprise; ASSUMPTION; ENTERPRISE",
		"// assume that edition is community; ASSUMPTION; COMMUNITY",
		"// assUMe that edition is comMunity; ASSUMPTION; COMMUNITY",
		"// ASSERT that edition is enterprise; ASSERTION; ENTERPRISE",
		"// assERT that edition is community; ASSERTION; COMMUNITY",
		"//assUMe that edition is community; ASSUMPTION; COMMUNITY",
		"//ASSERT that edition is enterprise; ASSERTION; ENTERPRISE",
	})
	void shouldParseEditionPreconditions(String value, Precondition.Type expectedType, Edition expectedEdition) {

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(EditionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((EditionPrecondition) precondition).getEdition()).isEqualTo(expectedEdition);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that edition is something",
		"// assume that edition is ",
		"// assume that edition is",
	})
	void shouldFailOnWrongEditionPrecondition(String value) {
		assertThatIllegalArgumentException()
			.isThrownBy((() -> Precondition.parse(value)))
			.withMessage("Wrong edition precondition. Usage: `<assume|assert> that edition is <enterprise|community>`");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that RETURN TRUE; ASSUMPTION; TARGET",
		"// assert that RETURN TRUE; ASSERTION; TARGET",
		"// assUME that RETURN TRUE; ASSUMPTION; TARGET",
		"// assERT that RETURN TRUE; ASSERTION; TARGET",
		"//assume that RETURN TRUE; ASSUMPTION; TARGET",
		"//assert that RETURN TRUE; ASSERTION; TARGET",
		"// assume in target that RETURN TRUE; ASSUMPTION; TARGET",
		"// assume in schema that RETURN TRUE; ASSUMPTION; SCHEMA",
		"// assume in tARget that RETURN TRUE; ASSUMPTION; TARGET",
		"// assume in schEMa that RETURN TRUE; ASSUMPTION; SCHEMA",
	})
	void shouldParseCypherPreconditions(String value, Precondition.Type expectedType,
		QueryPrecondition.Database expectedTarget) {

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(QueryPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((QueryPrecondition) precondition).getDatabase()).isEqualTo(expectedTarget);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume in target that",
		"// assume that ",
		"// assume that",
	})
	void shouldFailOnWrongCypherPrecondition(String value) {
		assertThatIllegalArgumentException()
			.isThrownBy((() -> Precondition.parse(value)))
			.withMessage(
				"Wrong Cypher precondition. Usage: `<assume|assert> [in <target|schema>] that <cypher statement>`");
	}

	@ParameterizedTest
	@ValueSource(strings = "// Hello, not a precondition")
	void shouldIgnoreThingsThatAreNoPrecondition(String value) {

		Optional<Precondition> precondition = Precondition.parse(value);
		assertThat(precondition).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "// assume die welt ist schlecht", "// assert nix" })
	void shouldIgnoreCommentsStartingWithAssumeOrAssert(String value) {
		Optional<Precondition> precondition = Precondition.parse(value);
		assertThat(precondition).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({ "community, COMMUNITY", "enterprise, ENTERPRISE", ", UNKNOWN", "special, UNKNOWN" })
	void editionShouldBeDetectable(String value, Edition edition) {
		assertThat(
			EditionPrecondition.getEdition(new DefaultConnectionDetails(null, "Neo4j/4711", value, null, null, null)))
			.isEqualTo(edition);
	}
}
