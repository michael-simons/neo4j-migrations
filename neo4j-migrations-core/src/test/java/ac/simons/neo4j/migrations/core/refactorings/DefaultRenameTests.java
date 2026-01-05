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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.Optional;
import java.util.function.Function;

import ac.simons.neo4j.migrations.core.Neo4jVersion;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Query;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class DefaultRenameTests {

	private final Function<String, Optional<String>> elementExtractor = q -> Optional.of("n");

	@Test
	void simpleLabelRename() {

		DefaultRename rename = (DefaultRename) Rename.label("Movie", "Film");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (s:Movie) REMOVE s:Movie SET s:Film");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("3.5");
	}

	@Test
	void shouldEscapeIfNecessary() {

		DefaultRename rename = (DefaultRename) Rename.label("Le Movie", "Der Film");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (s:`Le Movie`) REMOVE s:`Le Movie` SET s:`Der Film`");
	}

	@Test
	void shouldNotEscapeMultipleTimes() {

		DefaultRename rename = (DefaultRename) Rename.label("Le Movie", "Der Film").inBatchesOf(23).inBatchesOf(null);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (s:`Le Movie`) REMOVE s:`Le Movie` SET s:`Der Film`");
	}

	@Test
	void simpleLabelRenameWithBatchSize() {

		DefaultRename rename = (DefaultRename) Rename.label("Film", "Movie").inBatchesOf(23);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (s:Film) CALL { WITH s REMOVE s:Film SET s:Movie } IN TRANSACTIONS OF 23 ROWS");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("4.4");
	}

	@Test
	void labelRenameWithCustomQuery() {
		DefaultRename rename = (DefaultRename) Rename.label("Movie", "Film")
			.withCustomQuery("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n } WITH n AS s REMOVE s:Movie SET s:Film");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("4.1");
	}

	@Test
	void labelRenameWithCustomQueryAndBatchSize() {
		DefaultRename rename = (DefaultRename) Rename.label("Film", "Movie")
			.withCustomQuery("MATCH (n) WHERE n.title =~ '.*Matrix.*' RETURN n")
			.inBatchesOf(23);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n) WHERE n.title =~ '.*Matrix.*' RETURN n } WITH n AS s CALL { WITH s REMOVE s:Film SET s:Movie } IN TRANSACTIONS OF 23 ROWS");
	}

	@Test
	void simpleTypeRename() {
		DefaultRename rename = (DefaultRename) Rename.type("ACTED_IN", "HAT_GESPIELT_IN");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (a)-[old:ACTED_IN]->(b) CREATE (a)-[new:HAT_GESPIELT_IN]->(b) SET new+=old DELETE old");
	}

	@Test
	void typeRenameWithCustomQuery() {
		DefaultRename rename = (DefaultRename) Rename.type("ACTED_IN", "HAT_GESPIELT_IN")
			.withCustomQuery("MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n } WITH n AS old, startNode(n) AS a, endNode(n) AS b CREATE (a)-[new:HAT_GESPIELT_IN]->(b) SET new+=old DELETE old");
	}

	@Test
	void simpleTypeRenameWithBatchSize() {
		DefaultRename rename = (DefaultRename) Rename.type("HAT_GESPIELT_IN", "ACTED_IN").inBatchesOf(42);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"MATCH (a)-[old:HAT_GESPIELT_IN]->(b) CALL { WITH old, a, b CREATE (a)-[new:ACTED_IN]->(b) SET new+=old DELETE old } IN TRANSACTIONS OF 42 ROWS");
	}

	@Test
	void typeRenameWithCustomQueryAndBatchSize() {
		DefaultRename rename = (DefaultRename) Rename.type("HAT_GESPIELT_IN", "ACTED_IN")
			.inBatchesOf(42)
			.withCustomQuery("MATCH (n:Movie) <-[r:HAT_GESPIELT_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) <-[r:HAT_GESPIELT_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n } WITH n AS old, startNode(n) AS a, endNode(n) AS b CALL { WITH old, a, b CREATE (a)-[new:ACTED_IN]->(b) SET new+=old DELETE old } IN TRANSACTIONS OF 42 ROWS");
	}

	@Test
	void simpleNodePropertyRename() {

		DefaultRename rename = (DefaultRename) Rename.nodeProperty("released", "veröffentlicht in");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"MATCH (s) WHERE s.released IS NOT NULL SET s.`veröffentlicht in` = s.released REMOVE s.released");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("3.5");
	}

	@Test
	void simpleNodePropertyRenameWithBatchSize() {

		DefaultRename rename = (DefaultRename) Rename.nodeProperty("veröffentlicht in", "released").inBatchesOf(23);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"MATCH (s) WHERE s.`veröffentlicht in` IS NOT NULL CALL { WITH s SET s.released = s.`veröffentlicht in` REMOVE s.`veröffentlicht in` } IN TRANSACTIONS OF 23 ROWS");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("4.4");
	}

	@Test
	void nodePropertyWithCustomQuery() {

		DefaultRename rename = (DefaultRename) Rename.nodeProperty("released", "veröffentlicht in")
			.withCustomQuery("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n } WITH n AS s WHERE s.released IS NOT NULL SET s.`veröffentlicht in` = s.released REMOVE s.released");
		assertThat(rename.getFeatures().requiredVersion()).isEqualTo("4.1");
	}

	@Test
	void nodePropertyWithCustomQueryAndBatchSize() {

		DefaultRename rename = (DefaultRename) Rename.nodeProperty("veröffentlicht in", "released")
			.withCustomQuery("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n")
			.inBatchesOf(23);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n } WITH n AS s WHERE s.`veröffentlicht in` IS NOT NULL CALL { WITH s SET s.released = s.`veröffentlicht in` REMOVE s.`veröffentlicht in` } IN TRANSACTIONS OF 23 ROWS");
	}

	@Test
	void simpleRelPropertyRename() {
		DefaultRename rename = (DefaultRename) Rename.relationshipProperty("roles", "die rollen");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo("MATCH (a)-[r]->(b) WHERE r.roles IS NOT NULL SET r.`die rollen` = r.roles REMOVE r.roles");
	}

	@Test
	void relPropertyRenameWithCustomQuery() {
		DefaultRename rename = (DefaultRename) Rename.relationshipProperty("roles", "die rollen")
			.withCustomQuery("MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n } WITH n AS r WHERE r.roles IS NOT NULL SET r.`die rollen` = r.roles REMOVE r.roles");
	}

	@Test
	void simpleRelPropertyRenameWithBatchSize() {
		DefaultRename rename = (DefaultRename) Rename.relationshipProperty("die rollen", "roles").inBatchesOf(42);

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"MATCH (a)-[r]->(b) WHERE r.`die rollen` IS NOT NULL CALL { WITH r SET r.roles = r.`die rollen` REMOVE r.`die rollen` } IN TRANSACTIONS OF 42 ROWS");
	}

	@Test
	void relPropertyRenameWithCustomQueryAndBatchSize() {
		DefaultRename rename = (DefaultRename) Rename.relationshipProperty("die rollen", "roles")
			.inBatchesOf(42)
			.withCustomQuery("MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n");

		assertThat(rename.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, this.elementExtractor))
			.extracting(Query::text)
			.isEqualTo(
					"CALL { MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n } WITH n AS r WHERE r.`die rollen` IS NOT NULL CALL { WITH r SET r.roles = r.`die rollen` REMOVE r.`die rollen` } IN TRANSACTIONS OF 42 ROWS");
	}

}
