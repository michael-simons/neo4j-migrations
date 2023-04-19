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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrationChainTest {

	static final ConnectionDetails CONNECTION_DETAILS = new DefaultConnectionDetails("aura", "hidden", null, "j", null, null);

	@Test
	void shouldIncludeConnectionInfo() {

		MigrationChain chain = MigrationChain.empty(CONNECTION_DETAILS);
		assertThat(chain.prettyPrint())
			.contains("j@aura (hidden)");
	}

	@Test
	void shouldNotPrintEmptyDatabase() {

		MigrationChain chain = MigrationChain.empty(CONNECTION_DETAILS);
		assertThat(chain.prettyPrint())
			.doesNotContain("Database:")
			.doesNotContain("Schema database: ")
			.contains("No migrations found.");
	}

	@Test
	void shouldPrintDatabase() {

		MigrationChain chain = new DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", null), Collections.emptyMap());
		assertThat(chain.prettyPrint()).contains("Database: a");
	}

	@Test
		// GH-648
	void shouldPrintEdition() {

		MigrationChain chain = new DefaultMigrationChain(new DefaultConnectionDetails("aura", "6.66", "Special", "j", "a", null), Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("j@aura (6.66 Special Edition)");
	}

	@Test
	void shouldPrintSchemaDatabase() {

		MigrationChain chain = new DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", "b"), Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.contains("Schema database: b");
	}

	@Test
	void shouldNotPrintSameSchemaDatabase() {

		MigrationChain chain = new DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", "a"), Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.doesNotContain("Schema database: a");
	}

	@SuppressWarnings("TextBlockMigration")
	@Test
	void shouldPrettyPrintElements() {

		MigrationChain chain = new DefaultMigrationChain(CONNECTION_DETAILS,
			Map.of(MigrationVersion.withValue("1"), ChainToolTest.pendingMigration("1", "C1")));
		assertThat(chain.prettyPrint())
			.isEqualTo(
				"\n" +
				"j@aura (hidden)\n" +
				"\n" +
				"+---------+---------------+--------+--------------+----+----------------+---------+----------+\n" +
				"| Version | Description   | Type   | Installed on | by | Execution time | State   | Source   |\n" +
				"+---------+---------------+--------+--------------+----+----------------+---------+----------+\n" +
				"| 1       | a description | CYPHER |              |    |                | PENDING | 1.cypher |\n" +
				"+---------+---------------+--------+--------------+----+----------------+---------+----------+\n"
			);
	}

	@Nested
	class Elements {
		@Test
		void toMapShouldWork() {
			var element = ChainToolTest.appliedMigration("1", "C1").asMap();
			assertThat(element)
				.contains(
					Map.entry("version", "1"),
					Map.entry("description", "a description"),
					Map.entry("type", "CYPHER"),
					Map.entry("installedBy", "Der Mann Panik Panzer/Danger Dan"),
					Map.entry("executionTime", "PT1H18M31S"),
					Map.entry("source", "foobar.cypher"),
					Map.entry("state", "APPLIED")
				);
			assertThat(element)
				.containsOnlyKeys(
					"version",
					"description",
					"type",
					"installedOn",
					"installedBy",
					"executionTime",
					"state",
					"source"
				);
		}
	}
}
