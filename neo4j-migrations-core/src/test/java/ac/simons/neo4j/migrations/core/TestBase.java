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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SkipArm64IncompatibleConfiguration.class)
abstract class TestBase {

	static final String DEFAULT_NEO4J_IMAGE = System.getProperty("migrations.default-neo4j-image");

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = new Neo4jContainer(DEFAULT_NEO4J_IMAGE)
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withPlugins("apoc")
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

		clearDatabase(driver, null);
	}

	String getServerAddress() {

		return neo4j.getBoltUrl().replaceAll("bolt://", "");
	}

	boolean isModernNeo4j(ConnectionDetails connectionDetails) {
		var serverVersion = connectionDetails.getServerVersion();
		return serverVersion.startsWith("Neo4j/5") || serverVersion.matches("Neo4j/20\\d{2}\\.\\d{2}.*");
	}

	@AfterAll
	void closeDriver() {

		driver.close();
	}

	static void clearDatabase(Driver driver, String database) {

		SessionConfig sessionConfig = getSessionConfig(database);

		List<String> constraintsToBeDropped;
		List<String> indexesToBeDropped;
		try (Session session = driver.session(sessionConfig)) {
			session.run("MATCH (n) DETACH DELETE n");
			constraintsToBeDropped = session.run("SHOW CONSTRAINTS YIELD name RETURN 'DROP CONSTRAINT ' + name as cmd").list(r -> r.get("cmd").asString());
			indexesToBeDropped = session.run("SHOW INDEXES YIELD name RETURN 'DROP INDEX ' + name as cmd").list(r -> r.get("cmd").asString());
		}

		constraintsToBeDropped.forEach(cmd -> dropConstraint(driver, database, cmd));
		indexesToBeDropped.forEach(cmd -> dropIndex(driver, database, cmd));
	}

	static int lengthOfMigrations(Driver driver, String database) {
		return allLengthOfMigrations(driver, database).getOrDefault("<default>", 0);
	}

	static Map<String, Integer> allLengthOfMigrations(Driver driver, String database) {

		try (Session session = driver.session(getSessionConfig(database))) {
			return session.run(""
				+ "MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:`__Neo4jMigration`) "
				+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) "
				+ "RETURN b.migrationTarget as migrationTarget, length(p) AS l")
				.stream()
				.collect(Collectors.toMap(r -> r.get("migrationTarget").asString("<default>"), r -> r.get("l").asInt()));
		}
	}

	static SessionConfig getSessionConfig(String database) {

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
			assertThat(session.executeWrite(t -> t.run(constraint).consume()).counters().constraintsRemoved()).isNotZero();
		} catch (Neo4jException e) {
		}
	}

	static void dropIndex(Driver driver, String database, String index) {
		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			assertThat(session.executeWrite(t -> t.run(index).consume()).counters().indexesRemoved()).isNotZero();
		} catch (Neo4jException e) {
		}
	}
}
