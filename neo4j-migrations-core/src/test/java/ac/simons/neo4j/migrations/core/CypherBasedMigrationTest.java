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

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class CypherBasedMigrationTest {

	@ParameterizedTest // GH-232
	@ValueSource(strings = {"unix", "dos", "mac"})
	void unixDosOrMacLineEndingsMustNotChangeChecksum(String type)  {

		URL resource = CypherBasedMigrationTest.class.getResource("/V0001__" + type + ".cypher");

		CypherBasedMigration migration = new CypherBasedMigration(resource);
		List<String> statements = migration.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n) RETURN count(n) AS n");
		assertThat(migration.getChecksum()).hasValue("1902097523");
	}

	@ParameterizedTest // GH-238
	@ValueSource(strings = {"unix", "dos"})
	void shouldConvertLineEndings(String type)  {

		URL resource = CypherBasedMigrationTest.class.getResource("/ml/" + type + "/V0001__Just a couple of matches.cypher");

		CypherBasedMigration migration = new CypherBasedMigration(resource, true);
		List<String> statements = migration.getStatements();

		assertThat(statements).hasSize(3).containsOnly("MATCH (n)\nRETURN count(n) AS n");
		assertThat(migration.getChecksum()).hasValue("1916418554");
	}

	@Test
	void shouldBeAbleToReadFromJar() {

		// Those are in eu.michael-simons.neo4j.neo4j-migrations:test-migrations
		URL resource = CypherBasedMigrationTest.class.getResource("/some/changeset/V0002__create_new_data.cypher");
		CypherBasedMigration migration = new CypherBasedMigration(resource);

		List<String> statements = migration.getStatements();
		assertThat(statements).containsExactly("CREATE (n:FixedData) RETURN n", "MATCH (n) RETURN count(n) AS foobar");
	}

	@Test
	void shouldHandleMultilineStatements() {

		URL resource = CypherBasedMigrationTest.class
			.getResource("/my/awesome/migrations/moreStuff/V007__BondTheNameIsBond.cypher");
		CypherBasedMigration migration = new CypherBasedMigration(resource);
		List<String> statements = migration.getStatements();
		assertThat(statements).hasSize(2);
	}

	@Test
	void shouldComputeCheckSum() {

		URL resource = CypherBasedMigrationTest.class.getResource("/some/changeset/V0001__delete_old_data.cypher");

		CypherBasedMigration migration = new CypherBasedMigration(resource);

		assertThat(migration.getChecksum()).hasValue("1100083332");
	}
}
