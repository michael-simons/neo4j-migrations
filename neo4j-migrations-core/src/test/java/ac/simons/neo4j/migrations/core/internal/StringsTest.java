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
package ac.simons.neo4j.migrations.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
final class StringsTest {

	@ParameterizedTest
	@CsvSource({
		"DAS_IST_EIN_TEST,dasIstEinTest",
		"DAS,das",
		"DAS__AUCH,dasAuch",
		"dasEbenfalls,dasEbenfalls",
		"dasEBenfalls,dasEbenfalls"
	})
	void toCamelCaseShouldWork(String in, String expected) {
		assertThat(Strings.toCamelCase(in)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"// Simple comment",
		"  // not so simple",
		" //foo    ",
		" //foo    \n",
		" //foo  \n ",
		" //foo \r\n "
	})
	void shouldDetectSingleLineComment(String in) {
		assertThat(Strings.isSingleLineComment(in)).isTrue();
	}

	@Test
	void shouldRequireValue() {
		assertThatNullPointerException().isThrownBy(() -> Strings.isSingleLineComment(null))
			.withMessage("Statement to check must not be null");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t\t", "NULL"})
	void isBlankShouldWork(String value) {

		assertThat(Strings.isBlank("NULL".equals(value) ? null : value)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t\t", "NULL", "a"})
	void escapeIfNecessaryShouldBailOutEarly(String value) {

		String valueUsed = "NULL".equals(value) ? null : value;
		assertThat(Strings.escapeIfNecessary(valueUsed)).isSameAs(valueUsed);
	}

	@ParameterizedTest
	@CsvSource({
		"ABC, ABC",
		"A C, `A C`",
		"A` C, `A`` C`",
		"A`` C, `A`` C`",
		"ALabel, ALabel",
		"A Label, `A Label`",
		"A `Label, `A ``Label`",
		"`A `Label, ```A ``Label`",
		"Spring Data Neo4j⚡️RX, `Spring Data Neo4j⚡️RX`"
	})
	void shouldEscapeProper(String in, String expected) {
		String value = Strings.escapeIfNecessary(in);
		assertThat(value).isEqualTo(expected);
	}

	@CsvSource(delimiterString = "@@", value = {
		"RETURN toUpper(elementId(n))@@ RETURN toUpper(toString(id(n)))",
		"RETURN toUpper(elementId( n ))@@ RETURN toUpper(toString(id( n )))",
		"RETURN elementId(n)@@ RETURN toString(id(n))",
		"RETURN id(n)@@ RETURN id(n)"
	})
	void replaceElementIdCallsShouldWork(String query, String expected) {

		String newQuery = Strings.replaceElementIdCalls(query);
		assertThat(newQuery).isEqualTo(expected);
	}

	@Nested
	class Capitalization {

		@Test
		void shouldDealWithNull() {
			assertThat(Strings.capitalize(null)).isNull();
		}

		@Test
		void shouldDealWithEmpty() {

			String val = " ";
			assertThat(Strings.capitalize(val)).isSameAs(val);
		}

		@Test
		void shouldNotFixThingsThatDontNeedFixing() {

			String val = "Abc";
			assertThat(Strings.capitalize(val)).isSameAs(val);
		}

		@Test
		void shouldCapitalize() {

			String val = "abc";
			assertThat(Strings.capitalize(val)).isEqualTo("Abc");
		}
	}
}
