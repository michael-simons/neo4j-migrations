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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
class PreconditionTests {

	// copied from Precondition#formattedHint
	private static String formattedHint(String hint) {
		return "`" + hint.replace("//", "").trim() + "`";
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = { "// assume that version is 3.4; ASSUMPTION; 3.4",
			"// assUMe that version is 3.4 or 3.5; ASSUMPTION; 3.4, 3.5",
			"// assume that vErsion is 3.4, 3.5, 4.4 or 5.0; ASSUMPTION; 3.4, 3.5, 4.4, 5.0",
			"// ASSERT that version is 3.4, 3.5, 4.4 or 5.0; ASSERTION; 3.4, 3.5, 4.4, 5.0",
			"// assert that VERSION is 3.4,3.5, 4.4 or 5.0; ASSERTION; 3.4, 3.5, 4.4, 5.0",
			"// ASSERT that Version is 3.4; ASSERTION; 3.4", "//assume that version is 3.4; ASSUMPTION; 3.4",
			"//assert that version is 3.4; ASSERTION; 3.4", "//assert that version is 3 or 4; ASSERTION; 3, 4",
			"//assert that version is lt 4.0; ASSERTION; 4.0", "//assert that version is ge 4.0; ASSERTION; 4.0",
			"//assert that version is ge 4.0.0; ASSERTION; 4.0.0",
			"//assert that version is lt 4.0.1.1; ASSERTION; 4.0.1.1", "// assert that version is 3; ASSERTION; 3" })
	void shouldParseVersionPreconditions(String value, Precondition.Type expectedType, String expectedVersions) {

		Set<String> expectedVersions_ = Arrays.stream(expectedVersions.split(","))
			.map(String::trim)
			.map(s -> "Neo4j/" + s)
			.collect(Collectors.toCollection(TreeSet::new));

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(VersionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((VersionPrecondition) precondition).getVersions()).containsExactlyElementsOf(expectedVersions_);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = { "// assume that version is something", "// assume that version is ",
			"// assume that version is", "// assume that version is ," })
	void shouldFailOnWrongVersionPrecondition(String value) {
		assertThatIllegalArgumentException().isThrownBy((() -> Precondition.parse(value)))
			.withMessage("Wrong version precondition `" + value.replace("//", "").trim()
					+ "`. Usage: `<assume|assert> that version is <versions>`. With <versions> being a comma separated list of major.minor.patch versions.");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";",
			value = { "// assume that edition is enterprise; ASSUMPTION; ENTERPRISE",
					"// assume that edition is EnteRprise; ASSUMPTION; ENTERPRISE",
					"// assume that edition is community; ASSUMPTION; COMMUNITY",
					"// assUMe that Edition is comMunity; ASSUMPTION; COMMUNITY",
					"// ASSERT that EDITION is enterprise; ASSERTION; ENTERPRISE",
					"// assERT that edition is community; ASSERTION; COMMUNITY",
					"//assUMe that edition is community; ASSUMPTION; COMMUNITY",
					"//ASSERT that edition is enterprise; ASSERTION; ENTERPRISE" })
	void shouldParseEditionPreconditions(String value, Precondition.Type expectedType, Neo4jEdition expectedEdition) {

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(EditionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((EditionPrecondition) precondition).getEdition()).isEqualTo(expectedEdition);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "// assume that edition is something", "// assume that edition is ",
			"// assume that edition is" })
	void shouldFailOnWrongEditionPrecondition(String value) {
		assertThatIllegalArgumentException().isThrownBy((() -> Precondition.parse(value)))
			.withMessage("Wrong edition precondition " + formattedHint(value)
					+ ". Usage: `<assume|assert> that edition is <enterprise|community>`.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "// assume that neo4j is 4711", "// assume that q' RETURN false" })
	void shouldFailOnSomethingThatLooksLikeAPreconditionButIsnt(String value) {
		assertThatIllegalArgumentException().isThrownBy((() -> Precondition.parse(value)))
			.withMessage("Wrong precondition " + formattedHint(value)
					+ ". Supported are `<assume|assert> (that <edition|version>)|q' <cypherQuery>)`.");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";",
			value = { "// assume q'RETURN TRUE; ASSUMPTION; TARGET", "// assert q'RETURN TRUE; ASSERTION; TARGET",
					"// assUME q'RETURN TRUE; ASSUMPTION; TARGET", "// assERT q'RETURN TRUE; ASSERTION; TARGET",
					"//assume q'RETURN TRUE; ASSUMPTION; TARGET", "//assume q' RETURN TRUE; ASSUMPTION; TARGET",
					"//assert q'RETURN TRUE; ASSERTION; TARGET",
					"// assume in target q'RETURN TRUE; ASSUMPTION; TARGET",
					"// assume in schema q'RETURN TRUE; ASSUMPTION; SCHEMA",
					"// assume in tARget q'RETURN TRUE; ASSUMPTION; TARGET",
					"// assume in schEMa q'RETURN TRUE; ASSUMPTION; SCHEMA" })
	void shouldParseCypherPreconditions(String value, Precondition.Type expectedType,
			QueryPrecondition.Database expectedTarget) {

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(QueryPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(((QueryPrecondition) precondition).getDatabase()).isEqualTo(expectedTarget);
			assertThat(((QueryPrecondition) precondition).getQuery()).isEqualTo("RETURN TRUE");
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = { "//assert that version is ge 4; ASSERTION; true",
			"//assert that version is ge 4.0; ASSERTION; true", "//assert that version is ge 4.0.0; ASSERTION; true",
			"//assert that version is lt 3.99; ASSERTION; false", "//assert that version is lt 4; ASSERTION; false",
			"//assert that version is lt 4.0; ASSERTION; false",
			"//assert that version is lt 4.0.0; ASSERTION; false" })
	void versionRangePreconditionShouldWork(String value, Precondition.Type expectedType, boolean met) {
		MigrationContext ctx = new MigrationContext() {
			@Override
			public MigrationsConfig getConfig() {
				return null;
			}

			@Override
			public Driver getDriver() {
				return null;
			}

			@Override
			public SessionConfig getSessionConfig() {
				return null;
			}

			@Override
			public SessionConfig getSessionConfig(UnaryOperator<SessionConfig.Builder> configCustomizer) {
				return null;
			}

			@Override
			public Session getSchemaSession() {
				return null;
			}

			@Override
			public ConnectionDetails getConnectionDetails() {
				return new DefaultConnectionDetails(null, "Neo4j/4.4", null, null, null, null);
			}

			@Override
			public Catalog getCatalog() {
				return Catalog.empty();
			}
		};

		Optional<Precondition> optionalPrecondition = Precondition.parse(value);
		assertThat(optionalPrecondition).hasValueSatisfying(precondition -> {
			assertThat(precondition).isInstanceOf(VersionPrecondition.class);
			assertThat(precondition.getType()).isEqualTo(expectedType);
			assertThat(precondition.isMet(ctx)).isEqualTo(met);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = { "// assume in target q'", "// assume q' ", "// assume q'" })
	void shouldFailOnWrongCypherPrecondition(String value) {
		assertThatIllegalArgumentException().isThrownBy((() -> Precondition.parse(value)))
			.withMessage("Wrong Cypher precondition " + formattedHint(value)
					+ ". Usage: `<assume|assert> [in <target|schema>] q' <cypher statement>`.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "// Hello, not a precondition", "// assume die welt ist schlecht", "// assert nix" })
	void shouldIgnoreThingsThatAreNoPrecondition(String value) {

		Optional<Precondition> precondition = Precondition.parse(value);
		assertThat(precondition).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({ "community, COMMUNITY", "enterprise, ENTERPRISE", ", UNDEFINED", "special, UNDEFINED" })
	void editionShouldBeDetectable(String value, String edition) {
		assertThat(EditionPrecondition
			.getEdition(new DefaultConnectionDetails(null, "Neo4j/4711", value, null, null, null)))
			.isEqualTo(Neo4jEdition.valueOf(edition));
	}

	@Nested
	class ToString {

		@Test
		void ofEditionPrecondition() {

			assertThat(Precondition.parse("// assume that edition is enterprise").map(Precondition::toString))
				.hasValue("// assume that edition is ENTERPRISE");
		}

		@Test
		void ofVersionPrecondition() {

			assertThat(Precondition.parse("// assume that version is 3.5, 4.0 or 4.4").map(Precondition::toString))
				.hasValue("// assume that version is 3.5, 4.0, 4.4");
		}

		@Test // 446
		void ofVersionPreconditionLt() {

			assertThat(Precondition.parse("// assume that version is lt 4.0").map(Precondition::toString))
				.hasValue("// assume that version is lt 4.0");
		}

		@Test // 446
		void ofVersionPreconditionGe() {

			assertThat(Precondition.parse("// assume that version is ge 4.0").map(Precondition::toString))
				.hasValue("// assume that version is ge 4.0");
		}

		@Test
		void ofQueryPrecondition() {

			assertThat(Precondition.parse("// assume q' RETURN false").map(Precondition::toString))
				.hasValue("// assume in TARGET q'RETURN false");
		}

	}

}
