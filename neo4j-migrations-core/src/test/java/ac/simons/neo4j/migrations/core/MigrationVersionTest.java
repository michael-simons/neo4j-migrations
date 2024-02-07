/*
 * Copyright 2020-2024 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class MigrationVersionTest {

	@ParameterizedTest
	@CsvSource(delimiter = ':', textBlock = """
		V1__a:1:a
		V021__HalloWelt:021:HalloWelt
		V021_1__HalloWelt:021.1:HalloWelt
		V021.1__HalloWelt:021.1:HalloWelt
		V021.1.2.4__HalloWelt:021.1.2.4:HalloWelt
		V021_1_2_4__HalloWelt:021.1.2.4:HalloWelt
		""")
	void shouldHandleCorrectClassNames(String name, String value, String description) {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse(name);
		assertThat(migrationVersion.getValue()).isEqualTo(value);
		assertThat(migrationVersion.getOptionalDescription()).hasValue(description);
	}

	@ParameterizedTest
	@CsvSource(delimiter = ':', textBlock = """
		V1__a.cypher:1:a
		V021__HalloWelt.cypher:021:HalloWelt
		V4711__MirFallenKeineNamenEin.cypher:4711:MirFallenKeineNamenEin
		V4711__Ein Dateiname.cypher:4711:Ein Dateiname
		""")
	void shouldIgnoreCypherExtension(String name, String value, String description) {
		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse(name);
		assertThat(migrationVersion.getValue()).isEqualTo(value);
		assertThat(migrationVersion.getOptionalDescription()).hasValue(description);
	}

	@Test
	void shouldStripUnderscores() {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse("V1__a_b");
		assertThat(migrationVersion.getOptionalDescription()).hasValue("a b");
	}

	@ParameterizedTest
	@ValueSource(strings = {"HalloWelt", "V1", "V1__", "V__HalloWelt", "V1_HalloWelt", "V_1_HalloWelt",
		"V1_1_HalloWelt",
		"V1.1_HalloWelt", "V1..1__HalloWelt", "V1.1_2__HalloWelt", "V1_1.2__HalloWelt", "V1_1_2_HalloWelt",
		"V1.1.2_HalloWelt"
	})
	void shouldFailOnIncorrectClassNames(String value) {
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse(value));
	}

	@Test
	void shouldDetectRepeatableVersions() {

		MigrationVersion version = MigrationVersion.parse("R1__a_b");
		assertThat(version.getOptionalDescription()).hasValue("a b");
		assertThat(version.isRepeatable()).isTrue();
	}

	@Test
	void versionsShouldNotBeRepeatableByDefault() {

		MigrationVersion version = MigrationVersion.parse("V1__a_b");
		assertThat(version.getOptionalDescription()).hasValue("a b");
		assertThat(version.isRepeatable()).isFalse();
	}

	@ParameterizedTest // GH-1212
	@CsvSource(delimiter = ';', textBlock = """
		LEXICOGRAPHIC;0.0.0, 1.16.1, 1.17.1, 1.17.10, 1.17.2.3, 1.4.1, 1.4.2, 1.4.3, 2.0.0
		SEMANTIC;0.0.0, 1.4.1, 1.4.2, 1.4.3, 1.16.1, 1.17.1, 1.17.2.3, 1.17.10, 2.0.0
		""")
	void versionOrderShouldBeCorrect(MigrationsConfig.VersionSortOrder versionSortOrder, String expected) {
		var files = List.of(
			"V1.4.3__one.cypher",
			"V0.0.0__one.cypher",
			"V1.4.2__one.cypher",
			"V1.4.1__one.cypher",
			"V2.0.0__one.cypher",
			"V1.16.1__one.cypher",
			"V1_17_1__one.cypher",
			"V1_17_10__one.cypher",
			"V1_17_2_3__one.cypher"
		);
		var versions = files.stream().map(MigrationVersion::parse)
			.sorted(MigrationsConfig.builder().withVersionSortOrder(versionSortOrder).build().getVersionComparator())
			.map(MigrationVersion::getValue)
			.toList();
		assertThat(versions)
			.containsExactly(expected.split(", "));
	}

	@Test // GH-1212
	void equalVersion() {

		var comparator = MigrationVersion.getComparator(MigrationsConfig.VersionSortOrder.SEMANTIC);
		var v1 = MigrationVersion.parse("V1.0.0__a");
		var v2 = MigrationVersion.parse("V1.0.0__b");
		assertThat(comparator.compare(v1, v2)).isZero();
	}

	@Test // GH-1212
	void greaterSameNumberOfDigits() {

		var comparator = MigrationVersion.getComparator(MigrationsConfig.VersionSortOrder.SEMANTIC);
		var v1 = MigrationVersion.parse("V2.0.0__a");
		var v2 = MigrationVersion.parse("V1.0.0__b");
		assertThat(comparator.compare(v1, v2)).isOne();
	}

	@Test // GH-1212
	void smallerSameNumberOfDigits() {

		var comparator = MigrationVersion.getComparator(MigrationsConfig.VersionSortOrder.SEMANTIC);
		var v1 = MigrationVersion.parse("V1.0.0__a");
		var v2 = MigrationVersion.parse("V2.0.0__b");
		assertThat(comparator.compare(v1, v2)).isNegative();
	}

	@Test // GH-1212
	void differentNumberOfDigits() {

		var comparator = MigrationVersion.getComparator(MigrationsConfig.VersionSortOrder.SEMANTIC);
		var v1 = MigrationVersion.parse("V2.1__x");
		var v2 = MigrationVersion.parse("V2__x");
		var v3 = MigrationVersion.parse("V3.0__x");
		var v4 = MigrationVersion.parse("V3__x");
		var v5a = MigrationVersion.parse("V5__a");
		var v5b = MigrationVersion.parse("V5__b");

		var hlp = new TreeSet<>(comparator);
		hlp.addAll(List.of(v1, v2, v3, v4, v5a, v5b));
		assertThat(hlp).containsExactly(v2, v1, v3, v5a);
		assertThat(hlp).containsExactly(v2, v1, v4, v5a);
	}
}
