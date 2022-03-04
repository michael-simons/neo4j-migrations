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
		"// assume that neo4j is 3.4; ASSUMPTION",
		"// assUMe that neo4j is 3.4 or 3.5; ASSUMPTION",
		"// assume that neo4j is 3.4, 3.5, 4.4 or 5.0; ASSUMPTION",
		"// ASSERT that neo4j is 3.4, 3.5, 4.4 or 5.0; ASSERTION",
		"// assert that neo4j is 3.4,3.5, 4.4 or 5.0; ASSERTION",
		"// ASSERT that neo4j is 3.4; ASSERTION",
		"//assume that neo4j is 3.4; ASSUMPTION",
		"//assert that neo4j is 3.4; ASSERTION",
	})
	void shouldParseVersionPreconditions(String value, Precondition.Type expectedType) {

		Precondition precondition = Precondition.parse(value);
		assertThat(precondition).isNotNull();
		assertThat(precondition).isInstanceOf(VersionPrecondition.class);
		assertThat(precondition.getType()).isEqualTo(expectedType);
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
				.withMessage("Wrong version precondition. Usage: `<assume|assert> that neo4j is <versions>. With <versions> being a comma separated list.`");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
		"// assume that edition is enterprise; ASSUMPTION",
		"// assUMe that edition is community; ASSUMPTION",
		"// ASSERT that edition is enterprise; ASSERTION",
		"// assERT that edition is community; ASSERTION",
		"//assUMe that edition is community; ASSUMPTION",
		"//ASSERT that edition is enterprise; ASSERTION",
	})
	void shouldParseEditionPreconditions(String value, Precondition.Type expectedType) {

		Precondition precondition = Precondition.parse(value);
		assertThat(precondition).isNotNull();
		assertThat(precondition).isInstanceOf(EditionPrecondition.class);
		assertThat(precondition.getType()).isEqualTo(expectedType);
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
		"// assume that RETURN TRUE; ASSUMPTION",
		"// assert that RETURN TRUE; ASSERTION",
		"// assUME that RETURN TRUE; ASSUMPTION",
		"// assERT that RETURN TRUE; ASSERTION",
		"//assume that RETURN TRUE; ASSUMPTION",
		"//assert that RETURN TRUE; ASSERTION",
		"// assume in target that RETURN TRUE; ASSUMPTION",
		"// assume in schema that RETURN TRUE; ASSUMPTION",
	})
	void shouldParseCypherPreconditions(String value, Precondition.Type expectedType) {

		Precondition precondition = Precondition.parse(value);
		assertThat(precondition).isNotNull();
		assertThat(precondition).isInstanceOf(CypherPrecondition.class);
		assertThat(precondition.getType()).isEqualTo(expectedType);
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
				.withMessage("Wrong Cypher precondition. Usage: `<assume|assert> [in <target|schema>] that <cypher statement>`");
	}

	@ParameterizedTest
	@ValueSource(strings = "// Hello, not a precondition")
	void shouldIgnoreThingsThatAreNoPrecondition(String value) {

		Precondition precondition = Precondition.parse(value);
		assertThat(precondition).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "// assume die welt ist schlecht", "// assert nix" })
	void shouldIgnoreCommentsStartingWithAssumeOrAssert(String value) {
		Precondition precondition = Precondition.parse(value);
		assertThat(precondition).isNull();
	}
}
