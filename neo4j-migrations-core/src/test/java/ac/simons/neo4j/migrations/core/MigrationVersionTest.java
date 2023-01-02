/*
 * Copyright 2020-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class MigrationVersionTest {

	@ParameterizedTest
	@CsvSource(value = {"V1__a:1:a", "V021__HalloWelt:021:HalloWelt", "V021_1__HalloWelt:021.1:HalloWelt",
			"V021.1__HalloWelt:021.1:HalloWelt", "V021.1.2.4__HalloWelt:021.1.2.4:HalloWelt",
			"V021_1_2_4__HalloWelt:021.1.2.4:HalloWelt"}, delimiter = ':')
	void shouldHandleCorrectClassNames(String name, String value, String description) {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse(name);
		assertThat(migrationVersion.getValue()).isEqualTo(value);
		assertThat(migrationVersion.getOptionalDescription()).hasValue(description);
	}

	@ParameterizedTest
	@CsvSource(value = {"V1__a.cypher:1:a", "V021__HalloWelt.cypher:021:HalloWelt",
			"V4711__MirFallenKeineNamenEin.cypher:4711:MirFallenKeineNamenEin",
			"V4711__Ein Dateiname.cypher:4711:Ein Dateiname"}, delimiter = ':')
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
	@ValueSource(strings = {"HalloWelt", "V1", "V1__", "V__HalloWelt", "V1_HalloWelt", "V_1_HalloWelt", "V1_1_HalloWelt",
			"V1.1_HalloWelt", "V1..1__HalloWelt", "V1.1_2__HalloWelt", "V1_1.2__HalloWelt", "V1_1_2_HalloWelt", "V1.1.2_HalloWelt"
	})
	void shouldFailOnIncorrectClassNames(String value) {
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse(value));
	}

	@SuppressWarnings("deprecation")
	@Test
	void shouldDetectRepeatableVersions() {

		MigrationVersion version = MigrationVersion.parse("R1__a_b");
		assertThat(version.getOptionalDescription()).hasValue("a b");
		assertThat(version.isRepeatable()).isTrue();
	}

	@SuppressWarnings("deprecation")
	@Test
	void versionsShouldNotBeRepeatableByDefault() {

		MigrationVersion version = MigrationVersion.parse("V1__a_b");
		assertThat(version.getOptionalDescription()).hasValue("a b");
		assertThat(version.isRepeatable()).isFalse();
	}
}
