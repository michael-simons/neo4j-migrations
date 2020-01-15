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
package ac.simons.neo4j.migrations.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestBase {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private final Neo4jContainer neo4j = new Neo4jContainer<>("neo4j:4.0.0");

	Driver driver;

	@BeforeAll
	void initDriver() {

		Config config = Config.builder().withLogging(Logging.none()).build();

		neo4j.start();
		driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
	}

	@BeforeEach
	void clearDatabase() {

		clearDatabase(driver, null);
	}

	String getServerAddress() {

		return neo4j.getBoltUrl().replaceAll("bolt://", "");
	}

	@AfterAll
	void closeDriver() {

		driver.close();
		neo4j.stop();
	}

	static void clearDatabase(Driver driver, String database) {

		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			session.run("MATCH (n) DETACH DELETE n");
		}

		dropConstraint(driver, database, "DROP CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE");
		dropConstraint(driver, database, "DROP CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.name IS UNIQUE");
	}

	static int lengthOfMigrations(Driver driver, String database) {

		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			return session.run(""
				+ "MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:`__Neo4jMigration`) "
				+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) "
				+ "RETURN length(p) AS l").single().get("l").asInt();
		}
	}

	private static SessionConfig getSessionConfig(String database) {

		SessionConfig sessionConfig;
		if (database == null) {
			sessionConfig = SessionConfig.defaultConfig();
		} else {
			sessionConfig = SessionConfig.forDatabase(database);
		}
		return sessionConfig;
	}

	static void dropConstraint(Driver driver, String database, String constraint) {
		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			session.writeTransaction(t -> t.run(constraint).consume());
		} catch (Neo4jException e) {
		}
	}
}
