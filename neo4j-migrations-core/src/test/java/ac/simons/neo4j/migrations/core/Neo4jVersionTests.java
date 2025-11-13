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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class Neo4jVersionTests {

	@ParameterizedTest
	@ValueSource(strings = { "4.3", "Neo4j/4.3", "4.3.4711", "Neo4j/4.3.21", "Neo4j:4.3", "neo4j:4.3.23" })
	void shouldParseDespitePrefixAndSuffix(String value) {
		Neo4jVersion version = Neo4jVersion.of(value);
		assertThat(version).isEqualTo(Neo4jVersion.V4_3);
	}

	@ParameterizedTest
	@ValueSource(strings = { "5.0.0", "Neo4j/5.0.0", "5.0.0-drop04.0", "5.0.0-aura", "5.1", "5.2" })
	void shouldIdenfify5(String value) {
		Neo4jVersion version = Neo4jVersion.of(value);
		assertThat(version).isEqualTo(Neo4jVersion.V5);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Neo4j/4711", "2025.02.0-20138" })
	void shouldBeLatestOnAnythingHigherThanDefined(String value) {
		Neo4jVersion version = Neo4jVersion.of(value);
		assertThat(version).isEqualTo(Neo4jVersion.LATEST);
	}

	@Test
	void shouldIdentifyMajorVersion3() {
		assertThat(Neo4jVersion.V3_5.getMajorVersion()).isEqualTo(3);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "V3_5", "V5", "UNDEFINED", "LATEST" },
			mode = EnumSource.Mode.EXCLUDE)
	void shouldIdentifyMajorVersion4(Neo4jVersion version) {
		assertThat(version.getMajorVersion()).isEqualTo(4);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "V5", "UNDEFINED", "LATEST" }, mode = EnumSource.Mode.EXCLUDE)
	void shouldParseMinor(Neo4jVersion version) {

		assertThat(version.getMinorVersion()).isNotNegative();
	}

	@Test
	void shouldDealWithBlanks() {

		assertThat(Neo4jVersion.LATEST.sanitizeSchemaName(" ")).isEqualTo("` `");
		assertThat(Neo4jVersion.LATEST.sanitizeSchemaName("\t\t")).isEqualTo("`\t\t`");
		assertThat(Neo4jVersion.LATEST.sanitizeSchemaName(null)).isNull();
		String empty = "";
		assertThat(Neo4jVersion.LATEST.sanitizeSchemaName(empty)).isSameAs(empty);
		String a = "a";
		String firstResult = Neo4jVersion.LATEST.sanitizeSchemaName(a);
		assertThat(firstResult).isEqualTo("a");
		assertThat(Neo4jVersion.LATEST.sanitizeSchemaName("a")).isSameAs(firstResult);
	}

	@ParameterizedTest
	@CsvSource({ "ABC, ABC", "A C, `A C`", "A` C, `A`` C`", "A`` C, `A`` C`", "ALabel, ALabel", "A Label, `A Label`",
			"A `Label, `A ``Label`", "`A `Label, ```A ``Label`", "Spring Data Neo4j⚡️RX, `Spring Data Neo4j⚡️RX`",
			"Foo \u0060, `Foo ```", // This is the backtick itself in the string
			"Foo \\u0060, `Foo ```", // This is the backtick unicode escaped so that
										// without further processing `foo \u0060` would
										// end up at Cypher,
			"`, ````", "\u0060, ````", "```, ``````", "\u0060\u0060\u0060, ``````", "Hello`, `Hello```",
			"Hi````there, `Hi````there`", "Hi`````there, `Hi``````there`", "`a`b`c`, ```a``b``c```",
			"\u0060a`b`c\u0060d\u0060, ```a``b``c``d```" })
	void shouldEscapeProper(String in, String expected) {
		String value = Neo4jVersion.LATEST.sanitizeSchemaName(in);
		assertThat(value).isEqualTo(expected);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "V3_5", "V4_0", "V4_1", "UNDEFINED" },
			mode = EnumSource.Mode.INCLUDE)
	void oldVersionsShouldNotSupportOptions(Neo4jVersion version) {
		assertThat(version.supportsSchemaOptions()).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "V3_5", "V4_0", "V4_1", "UNDEFINED" },
			mode = EnumSource.Mode.EXCLUDE)
	void newVersionsShouldSupportOptions(Neo4jVersion version) {
		assertThat(version.supportsSchemaOptions()).isTrue();
	}

}
