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
package ac.simons.neo4j.migrations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class CypherBasedMigrationTest {

	@Test
	void shouldBeAbleToReadFromJar() throws IOException {

		// Those are in eu.michael-simons.neo4j.neo4j-migrations:test-migrations
		URL resource = CypherBasedMigrationTest.class.getResource("/some/changeset/V0002__create_new_data.cypher");
		CypherBasedMigration migration = new CypherBasedMigration(resource);

		List<String> statements = migration.readStatements();
		Assertions.assertAll(
			() -> Assertions.assertEquals(2, statements.size()),
			() -> Assertions.assertEquals("CREATE (n:FixedData) RETURN n", statements.get(0)),
			() -> Assertions.assertEquals("MATCH (n) RETURN count(n) AS foobar", statements.get(1))
		);
	}

	@Test
	void shouldHandleMultilineStatements() throws IOException {
		URL resource = CypherBasedMigrationTest.class
			.getResource("/my/awesome/migrations/moreStuff/V007__BondTheNameIsBond.cypher");
		CypherBasedMigration migration = new CypherBasedMigration(resource);
		List<String> statements = migration.readStatements();
		Assertions.assertEquals(2, statements.size());
	}
}
