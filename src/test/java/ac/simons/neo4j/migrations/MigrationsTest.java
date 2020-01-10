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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
class MigrationsTest extends TestBase {

	@Test
	void shouldFindMigrations() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1").build(), driver);

		List<Migration> listOfMigrations;
		listOfMigrations = migrations.findMigrations();
		Assertions.assertAll(
			() -> Assertions.assertEquals(2, listOfMigrations.size()),
			() -> Assertions.assertEquals("V001__FirstMigration", listOfMigrations.get(0).getDescription()),
			() -> Assertions.assertEquals("V002__AnotherMigration", listOfMigrations.get(1).getDescription())
		);
	}

	@Test
	void shouldFindStaticInnerClasses() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset3").build(), driver);

		List<Migration> listOfMigrations;
		listOfMigrations = migrations.findMigrations();
		Assertions.assertAll(
			() -> Assertions.assertEquals(1, listOfMigrations.size()),
			() -> Assertions.assertEquals("V003__InnerMigration", listOfMigrations.get(0).getDescription())
		);
	}

	@Test
	void shouldApplyMigrations() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1").build(), driver);
		migrations.apply();

		Assertions.assertEquals(2, lengthOfMigrations());

		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1", "ac.simons.neo4j.migrations.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();

		Assertions.assertEquals(3, lengthOfMigrations());
	}

	int lengthOfMigrations() {
		try (Session session = driver.session()) {
			return session.run(""
				+ "MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:`__Neo4jMigration`) "
				+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) "
				+ "RETURN length(p) AS l").single().get("l").asInt();
		}
	}
}
