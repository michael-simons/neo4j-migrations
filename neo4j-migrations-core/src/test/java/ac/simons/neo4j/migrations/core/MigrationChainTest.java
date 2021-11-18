/*
 * Copyright 2020-2021 the original author or authors.
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

	static class FakeChain implements MigrationChain {

		Optional<String> databaseName = Optional.empty();
		Optional<String> schemaDatabaseName = Optional.empty();

		@Override public String getServerAddress() {
			return null;
		}

		@Override public String getServerVersion() {
			return null;
		}

		@Override public String getUsername() {
			return null;
		}

		@SuppressWarnings("deprecation")
		@Override public String getDatabaseName() {
			return null;
		}

		@Override public Optional<String> getOptionalDatabaseName() {
			return databaseName;
		}

		@Override public Optional<String> getOptionalSchemaDatabaseName() {
			return schemaDatabaseName;
		}

		@Override public boolean isApplied(String version) {
			return false;
		}

		@Override public Collection<Element> getElements() {
			return Collections.emptyList();
		}
	}

	@Test
	void shouldNotPrintyEmptyDatabase() {
		FakeChain chain = new FakeChain();
		assertThat(chain.prettyPrint())
			.doesNotContain("Database:")
			.doesNotContain("Schema database: ")
			.contains("No migrations found.");
	}

	@Test
	void shouldPrintDatabase() {
		FakeChain chain = new FakeChain();
		chain.databaseName = Optional.of("a");
		assertThat(chain.prettyPrint()).contains("Database: a");
	}

	@Test
	void shouldPrintSchemaDatabase() {
		FakeChain chain = new FakeChain();
		chain.databaseName = Optional.of("a");
		chain.schemaDatabaseName = Optional.of("b");
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.contains("Schema database: b");
	}

	@Test
	void shouldNotPrintSameSchemaDatabase() {
		FakeChain chain = new FakeChain();
		chain.databaseName = Optional.of("a");
		chain.schemaDatabaseName = Optional.of("a");
		assertThat(chain.prettyPrint())
			.contains("Database: a")
			.doesNotContain("Schema database: a");
	}
}
