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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class DeleteResultTests {

	@Test
	void shouldCheckMigrationVersion() {
		var result = new DeleteResult(null, 423, 0, 0, 0, 0, null);
		assertThat(result.prettyPrint()).isEqualTo("Database is unchanged, no version has been deleted.");
	}

	@Test
	void shouldCheckMigrationNodeCount() {
		var result = new DeleteResult(null, 0, 0, 0, 0, 0, MigrationVersion.withValue("23"));
		assertThat(result.prettyPrint()).isEqualTo("Database is unchanged, no version has been deleted.");
	}

	@Test
	void shouldPrettyPrint() {
		var result = new DeleteResult(null, 42, 0, 0, 0, 0, MigrationVersion.withValue("23"));
		assertThat(result.prettyPrint()).isEqualTo(
				"Migration 23 has been removed (deleted 42 nodes and 0 relationships from the default database).");
	}

	@Test
	void shouldPrettyPrintFullDescription() {
		var result = new DeleteResult("x", 42, 0, 0, 0, 0,
				MigrationVersion.withValueAndDescription("23", "Nichts ist wie es scheint", false));
		assertThat(result.prettyPrint()).isEqualTo(
				"Migration 23 (\"Nichts ist wie es scheint\") has been removed (deleted 42 nodes and 0 relationships from `x`).");
	}

	@Test
	void shouldBeNullSafe() {
		var result = new DeleteResult(null, 423, 0, 0, 0, 0, null);
		assertThat(result.getAffectedDatabase()).isNotPresent();
		assertThat(result.getVersion()).isNotPresent();
	}

}
