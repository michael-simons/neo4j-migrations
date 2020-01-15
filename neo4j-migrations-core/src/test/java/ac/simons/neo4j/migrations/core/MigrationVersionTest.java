/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
public class MigrationVersionTest {

	@Test
	void shouldHandleCorrectClassNames() {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse("V1__a");
		assertThat(migrationVersion.getValue()).isEqualTo("1");
		assertThat(migrationVersion.getDescription()).isEqualTo("a");

		migrationVersion = MigrationVersion.parse("V021__HalloWelt");
		assertThat(migrationVersion.getValue()).isEqualTo("021");
		assertThat(migrationVersion.getDescription()).isEqualTo("HalloWelt");
	}

	@Test
	void shouldIgnoreCypherExtension() {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse("V1__a.cypher");
		assertThat(migrationVersion.getValue()).isEqualTo("1");
		assertThat(migrationVersion.getDescription()).isEqualTo("a");

		migrationVersion = MigrationVersion.parse("V021__HalloWelt.cypher");
		assertThat(migrationVersion.getValue()).isEqualTo("021");
		assertThat(migrationVersion.getDescription()).isEqualTo("HalloWelt");

		migrationVersion = MigrationVersion.parse("V4711__MirFallenKeineNamenEin.cypher");
		assertThat(migrationVersion.getValue()).isEqualTo("4711");
		assertThat(migrationVersion.getDescription()).isEqualTo("MirFallenKeineNamenEin");

		migrationVersion = MigrationVersion.parse("V4711__Ein Dateiname.cypher");
		assertThat(migrationVersion.getValue()).isEqualTo("4711");
		assertThat(migrationVersion.getDescription()).isEqualTo("Ein Dateiname");
	}

	@Test
	void shouldStripUnderscores() {

		MigrationVersion migrationVersion;
		migrationVersion = MigrationVersion.parse("V1__a_b");
		assertThat(migrationVersion.getDescription()).isEqualTo("a b");
	}

	@Test
	void shouldFailOnIncorrectClassNames() {

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse("HalloWelt"));
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse("V1"));
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse("V1__"));
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse("V__HalloWelt"));
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> MigrationVersion.parse("V1_HalloWelt"));
	}
}
