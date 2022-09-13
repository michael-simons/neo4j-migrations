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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrationChainTest {

	static final ConnectionDetails CONNECTION_DETAILS = new DefaultConnectionDetails("aura", "hidden", null, "j", null, null);

	@Test
	void shouldIncludeConnectionInfo() {

		MigrationChain chain = new ChainBuilder.DefaultMigrationChain(CONNECTION_DETAILS, Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("j@aura (hidden)");
	}

	@Test
	void shouldNotPrintEmptyDatabase() {

		MigrationChain chain = new ChainBuilder.DefaultMigrationChain(CONNECTION_DETAILS, Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.doesNotContain("Database:")
			.doesNotContain("Schema database: ")
			.contains("No migrations found.");
	}

	@Test
	void shouldPrintDatabase() {

		MigrationChain chain = new ChainBuilder.DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", null), Collections.emptyMap());
		assertThat(chain.prettyPrint()).contains("Database: a");
	}

	@Test
	void shouldPrintSchemaDatabase() {

		MigrationChain chain = new ChainBuilder.DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", "b"), Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.contains("Schema database: b");
	}

	@Test
	void shouldNotPrintSameSchemaDatabase() {

		MigrationChain chain = new ChainBuilder.DefaultMigrationChain(new DefaultConnectionDetails("aura", "hidden", null, "j", "a", "a"), Collections.emptyMap());
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.doesNotContain("Schema database: a");
	}
}
