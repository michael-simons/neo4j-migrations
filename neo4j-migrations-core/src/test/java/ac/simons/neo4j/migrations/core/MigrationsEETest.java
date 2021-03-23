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

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Tests that made only sense in Neo4j Enterprise Edition.
 *
 * @author Michael J. Simons
 * @soundtrack Paul van Dyk - From Then On
 */
@Testcontainers(disabledWithoutDocker = true)
class MigrationsEETest {

	private static final String TARGET_DATABASSE = "migrationTest";

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@Container
	private static final Neo4jContainer neo4j = new Neo4jContainer<>("neo4j:4.2-enterprise")
		.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");

	private static Driver driver;

	@BeforeAll
	static void initDriver() {

		Config config = Config.builder().withLogging(Logging.none()).build();
		driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {

			session.run("CREATE DATABASE migrationTest").consume();
		}
	}

	@Test
	void shouldRunInCorrectDatabase() {

		TestBase.clearDatabase(driver, TARGET_DATABASSE);

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase(TARGET_DATABASSE)
			.build(), driver);
		migrations.apply();

		// Assert basic working
		assertThat(TestBase.lengthOfMigrations(driver, TARGET_DATABASSE)).isEqualTo(2);

		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:my/awesome/migrations/moreStuff")
			.withDatabase(TARGET_DATABASSE)
			.build(), driver);
		migrations.apply();

		// Assert that verification runs in the correct database
		assertThat(TestBase.lengthOfMigrations(driver, TARGET_DATABASSE)).isEqualTo(8);

		try (Session session = driver.session(SessionConfig.forDatabase(TARGET_DATABASSE))) {

			// Assert that the cypher based migration was correctly applied
			long cnt = session.run("MATCH (agent:`007`) RETURN count(agent) AS cnt").single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);

			// Assert that the lock had been created in the correct database
			List<String> constraints = session.run(
				"CALL db.constraints() YIELD description "
					+ "WITH description WHERE description =~'.+:__Neo4jMigrationsLock\\\\s?\\\\).*' "
					+ "RETURN description"
			).list(r -> r.get("description").asString());
			assertThat(constraints).containsExactlyInAnyOrder(
				"CONSTRAINT ON ( __neo4jmigrationslock:__Neo4jMigrationsLock ) ASSERT (__neo4jmigrationslock.name) IS UNIQUE",
				"CONSTRAINT ON ( __neo4jmigrationslock:__Neo4jMigrationsLock ) ASSERT (__neo4jmigrationslock.id) IS UNIQUE"
			);
		}
	}
}
