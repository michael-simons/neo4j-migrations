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
package ac.simons.neo4j.migrations.formats.adoc;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsciiDoctorBasedMigrationProviderIT {

	@SuppressWarnings("resource")
	protected final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("migrations.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	Driver driver;

	@BeforeAll
	void initDriver() {
		Config config = Config.builder().withLogging(Logging.none()).build();

		neo4j.start();
		driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
	}

	@BeforeEach
	void clearDatabase() {
		List<String> constraintsToBeDropped;
		List<String> indexesToBeDropped;
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			constraintsToBeDropped = session.run("SHOW CONSTRAINTS YIELD name RETURN 'DROP CONSTRAINT ' + name as cmd")
				.list(r -> r.get("cmd").asString());
			indexesToBeDropped = session.run("SHOW INDEXES YIELD name RETURN 'DROP INDEX ' + name as cmd")
				.list(r -> r.get("cmd").asString());
		}

		constraintsToBeDropped.forEach(cmd -> dropConstraint(driver, cmd));
		indexesToBeDropped.forEach(cmd -> dropIndex(driver, cmd));
	}

	static void dropConstraint(Driver driver, String constraint) {
		try (Session session = driver.session()) {
			assertThat(
				session.executeWrite(t -> t.run(constraint).consume()).counters().constraintsRemoved()).isNotZero();
		} catch (Neo4jException ignored) {
		}
	}

	static void dropIndex(Driver driver, String index) {
		try (Session session = driver.session()) {
			assertThat(session.executeWrite(t -> t.run(index).consume()).counters().indexesRemoved()).isNotZero();
		} catch (Neo4jException ignored) {
		}
	}

	@AfterAll
	void closeDriver() {

		driver.close();
	}

	@Test
	void example1ShouldWork() {
		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("/neo4j/adoc-migrations").build(), driver);

		Optional<MigrationVersion> migrationVersion = migrations.apply();
		assertThat(migrationVersion)
			.map(MigrationVersion::getValue)
			.hasValue("4.0");

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements()).hasSize(5);

		try (Session session = driver.session()) {
			Long cnt = session.run("MATCH (f:Foo {name: 'fighters'}) RETURN count(f)").single().get(0).asLong();
			assertThat(cnt).isOne();
		}
	}

	@Test
	void readmeExampleShouldWork() {
		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("/neo4j/migrations-with-includes").build(), driver);

		Optional<MigrationVersion> migrationVersion = migrations.apply();
		assertThat(migrationVersion)
			.map(MigrationVersion::getValue)
			.hasValue("2.0");

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements()).hasSize(4);

		try (Session session = driver.session()) {
			Long cnt = session.run("MATCH (u:User {name: 'Michael'}) -[r:LIKES] -> () RETURN count(r)").single().get(0)
				.asLong();
			assertThat(cnt).isEqualTo(3L);
		}
	}
}
