/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Michael J. Simons
 */
class CleanIT extends TestBase {

	@BeforeEach
	void initDB() {
		try (Session session = this.driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			// Fake a couple of databases
			Stream.of(null, "db1", "db2").forEach(db -> {
				session.run(
						"create (:__Neo4jMigration {version: 'BASELINE', migrationTarget: $db}) - [:MIGRATED_TO] -> (:__Neo4jMigration {version: 'Next', migrationTarget: $db})",
						Values.parameters("db", db));
			});
			// Make one chain a bit longer
			session.run("MATCH (n:__Neo4jMigration {version: 'Next', migrationTarget: $db}) "
					+ "CREATE (n) - [:MIGRATED_TO] -> (:__Neo4jMigration {version: 'Next2', migrationTarget: $db})",
					Values.parameters("db", "db2"));
		}
		// Enforce creation of locks
		MigrationsLock migrationsLock = new MigrationsLock(
				new DefaultMigrationContext(MigrationsConfig.defaultConfig(), this.driver));
		try {
			migrationsLock.lock();
		}
		finally {
			migrationsLock.unlock();
		}

		assumeThat(numConstraints()).isEqualTo(2);
	}

	private int numConstraints() {
		try (Session session = this.driver.session()) {
			return session.run("SHOW CONSTRAINTS YIELD name RETURN count(*) AS numConstraints").single().get(0).asInt();
		}
	}

	@Test
	void cleanSelectedTargetShouldOnlyCleanThat() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withDatabase("db1").withSchemaDatabase("neo4j").build(),
				this.driver);

		CleanResult cleanResult = migrations.clean(false);

		assertThat(cleanResult.getChainsDeleted()).containsExactly("db1");
		assertThat(cleanResult.getConstraintsRemoved()).isZero();
		assertThat(cleanResult.getIndexesRemoved()).isZero();
		assertThat(cleanResult.getNodesDeleted()).isEqualTo(2);
		assertThat(cleanResult.getRelationshipsDeleted()).isEqualTo(1);
		assertThat(TestBase.allLengthOfMigrations(this.driver, "neo4j")).hasSize(2)
			.containsEntry("db2", 2)
			.containsEntry("<default>", 1);

		assertThat(numConstraints()).isEqualTo(2);
	}

	@Test
	void cleaningDefaultLeavesTheRest() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withSchemaDatabase("neo4j").build(), this.driver);

		CleanResult cleanResult = migrations.clean(false);

		assertThat(cleanResult.getChainsDeleted()).containsExactly("<default>");
		assertThat(cleanResult.getConstraintsRemoved()).isZero();
		assertThat(cleanResult.getIndexesRemoved()).isZero();
		assertThat(cleanResult.getNodesDeleted()).isEqualTo(2);
		assertThat(cleanResult.getRelationshipsDeleted()).isEqualTo(1);
		assertThat(TestBase.allLengthOfMigrations(this.driver, "neo4j")).hasSize(2)
			.containsEntry("db1", 1)
			.containsEntry("db2", 2);

		assertThat(numConstraints()).isEqualTo(2);
	}

	@Test
	void cleanAllCleansAll() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withSchemaDatabase("neo4j").build(), this.driver);

		CleanResult cleanResult = migrations.clean(true);

		assertThat(cleanResult.getChainsDeleted()).containsExactly("<default>", "db1", "db2");
		assertThat(cleanResult.getConstraintsRemoved()).isEqualTo(2);
		assertThat(cleanResult.getIndexesRemoved()).isZero();
		assertThat(cleanResult.getNodesDeleted()).isEqualTo(7);
		assertThat(cleanResult.getRelationshipsDeleted()).isEqualTo(4);
		assertThat(TestBase.allLengthOfMigrations(this.driver, "neo4j")).isEmpty();

		assertThat(numConstraints()).isZero();
	}

}
