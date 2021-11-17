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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@Container
	private static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.3-enterprise")
		.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");

	private static Driver driver;

	private static final Logger LOCK_LOGGER = Logger.getLogger(MigrationsLock.class.getName());
	private static final Level PREVIOUS_LOCK_LOGGING_LEVEL = LOCK_LOGGER.getLevel();
	private static final ConsoleHandler SIMPLE_CONSOLE_HANDLER = new ConsoleHandler();
	static {
		SIMPLE_CONSOLE_HANDLER.setLevel(Level.FINE);
		SIMPLE_CONSOLE_HANDLER.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return Instant.ofEpochMilli(record.getMillis()) + ": " + MessageFormat.format(record.getMessage(), record.getParameters()) + System.lineSeparator();
			}
		});
	}

	@BeforeAll
	static void initDriver() {

		Config config = Config.builder().withLogging(Logging.none()).build();
		driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			Stream.of("migrationTest", "schemaDatabase", "anotherTarget")
				.map(database -> Collections.<String, Object>singletonMap("database", database))
				.forEach(params -> session.run("CREATE DATABASE $database", params).consume());
		}
	}

	@BeforeAll
	static void enableConsoleLoggingOnLock() {
		LOCK_LOGGER.setLevel(Level.FINE);
		LOCK_LOGGER.addHandler(SIMPLE_CONSOLE_HANDLER);
	}

	@BeforeAll
	static void disableConsoleLoggingOnLock() {
		LOCK_LOGGER.setLevel(PREVIOUS_LOCK_LOGGING_LEVEL);
		LOCK_LOGGER.removeHandler(SIMPLE_CONSOLE_HANDLER);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "n/a", value = {
		"neo4j, n/a",
		"migrationTest, n/a",
		"migrationTest, neo4j",
		"migrationTest, schemaDatabase",
		"n/a, schemaDatabase",
		"neo4j, schemaDatabase",
		"n/a, n/a"
	})
	void shouldRunInCorrectDatabase(String targetDatabase, String schemaDatabase) {

		Logger logger = Logger.getLogger(MigrationsEETest.class.getName());
		String actualSchemaDatabase = schemaDatabase == null ? targetDatabase : schemaDatabase;
		String targetDatabaseInStats =
			schemaDatabase != null ? targetDatabase == null ? "neo4j" : targetDatabase : "<default>";

		logger.log(Level.INFO, "Target database {0}, schemaDatabase {1}", new Object[] { targetDatabase, actualSchemaDatabase });

		TestBase.clearDatabase(driver, targetDatabase);
		if (schemaDatabase != null) {
			TestBase.clearDatabase(driver, schemaDatabase);
		}

		// First application
		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase(targetDatabase)
			.withSchemaDatabase(schemaDatabase)
			.build(), driver);
		migrations.apply();

		// Assert that verification runs in the correct database
		assertThat(TestBase.allLengthOfMigrations(driver, actualSchemaDatabase)).containsEntry(targetDatabaseInStats, 2);

		// Second application
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:my/awesome/migrations/moreStuff")
			.withDatabase(targetDatabase)
			.withSchemaDatabase(schemaDatabase)
			.build(), driver);
		migrations.apply();

		// Assert that verification runs in the correct database
		assertThat(TestBase.allLengthOfMigrations(driver, actualSchemaDatabase)).containsEntry(targetDatabaseInStats, 8);

		// Assert that the cypher based migration was correctly applied
		try (Session session = targetDatabase == null ? driver.session() : driver.session(SessionConfig.forDatabase(targetDatabase))) {

			long cnt = session.run("MATCH (agent:`007`) RETURN count(agent) AS cnt").single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		}

		// Assert that the lock had been created in the correct database
		try (Session session = driver.session(TestBase.getSessionConfig(actualSchemaDatabase))) {

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

	@Test
	void usingSchemeWithPreexistingDatabasesMigrationsShouldWork() {

		TestBase.clearDatabase(driver, "migrationTest");
		Migrations migrationTestWithoutSchema = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("migrationTest")
			.build(), driver);
		migrationTestWithoutSchema.apply();

		Migrations migrationTestSameSchema = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("migrationTest")
			.withSchemaDatabase("migrationTest")
			.build(), driver);
		migrationTestSameSchema.apply();

		Map<String, Integer> allLengths = TestBase.allLengthOfMigrations(driver, "migrationTest");
		assertThat(allLengths).containsOnly(new AbstractMap.SimpleEntry<>("<default>", 2)); // There is none, since it maybe from an old migration

		Stream.of("migrationTest").forEach(databaseName -> {
			try (Session session = driver.session(SessionConfig.forDatabase(databaseName))) {
				long cnt = session.run("MATCH (n:IWasHere) RETURN count(n) AS cnt").single().get("cnt").asLong();
				assertThat(cnt).isEqualTo(1L);
			}
		});

		// Add a migration with a scheme
		Migrations migrationNeo4j = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("neo4j")
			.withSchemaDatabase("migrationTest")
			.build(), driver);
		migrationNeo4j.apply();

		// Now the default entry is there (a potential old one) and the neo4j one
		allLengths = TestBase.allLengthOfMigrations(driver, "migrationTest");
		assertThat(allLengths).containsOnly(
			new AbstractMap.SimpleEntry<>("<default>", 2),
			new AbstractMap.SimpleEntry<>("neo4j", 2)
		);

		migrationTestWithoutSchema.apply();
		migrationTestSameSchema.apply();

		allLengths = TestBase.allLengthOfMigrations(driver, "migrationTest");
		assertThat(allLengths).containsOnly(
			new AbstractMap.SimpleEntry<>("<default>", 2),
			new AbstractMap.SimpleEntry<>("neo4j", 2)
		);
	}

	@Test
	void shouldBeAbleToManageMultipleDatabasesFromOneSchemaDatabase() {

		String schemaDatabase = "schemaDatabase";
		TestBase.clearDatabase(driver, schemaDatabase);
		TestBase.clearDatabase(driver, "neo4j");
		TestBase.clearDatabase(driver, "migrationTest");
		TestBase.clearDatabase(driver, "anotherTarget");

		new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withSchemaDatabase(schemaDatabase)
			.build(), driver)
			.apply();

		// Explicit neo4j again
		new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("neo4j")
			.withSchemaDatabase(schemaDatabase)
			.build(), driver)
			.apply();

		new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("migrationTest")
			.withSchemaDatabase(schemaDatabase)
			.build(), driver)
			.apply();

		new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.withDatabase("anotherTarget")
			.withSchemaDatabase(schemaDatabase)
			.build(), driver)
			.apply();

		Map<String, Integer> allLengths = TestBase.allLengthOfMigrations(driver, schemaDatabase);
		assertThat(allLengths).containsEntry("neo4j", 2);
		assertThat(allLengths).containsEntry("migrationTest", 2);
		assertThat(allLengths).containsEntry("anotherTarget", 2);

		// Assert that the cypher based migration was correctly applied
		Stream.of("migrationTest", "anotherTarget").forEach(databaseName -> {
			try (Session session = driver.session(SessionConfig.forDatabase(databaseName))) {
				long cnt = session.run("MATCH (n:IWasHere) RETURN count(n) AS cnt").single().get("cnt").asLong();
				assertThat(cnt).isEqualTo(1L);
			}
		});

		new Migrations(MigrationsConfig.builder()
			.withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:my/awesome/migrations/moreStuff")
			.withDatabase("anotherTarget")
			.withSchemaDatabase(schemaDatabase)
			.build(), driver)
			.apply();

		allLengths = TestBase.allLengthOfMigrations(driver, schemaDatabase);
		assertThat(allLengths).containsEntry("neo4j", 2);
		assertThat(allLengths).containsEntry("migrationTest", 2);
		assertThat(allLengths).containsEntry("anotherTarget", 8);

		Stream.of("migrationTest", "neo4j", "anotherTarget").forEach(databaseName -> {
			try (Session session = driver.session(SessionConfig.forDatabase(databaseName))) {
				long cnt = session.run("MATCH (agent:`007`) RETURN count(agent) AS cnt").single().get("cnt").asLong();
				assertThat(cnt).isEqualTo(databaseName.equals("anotherTarget") ? 1L : 0L);
			}
		});
	}

	@ParameterizedTest
	@CsvSource(nullValues = "n/a", value = {
		"n/a, neo4j, n/a",
		"n/a, neo4j, schemaDatabase",
		"n/a, n/a, schemaDatabase",
		"n/a, n/a, n/a",
		"neo4j, neo4j, n/a",
		"neo4j, neo4j, schemaDatabase",
		"neo4j, n/a, schemaDatabase",
		"neo4j, n/a, n/a",
	})
	void lockShouldFailBecauseLockNodeExists(String database1, String database2, String schemaDatabase) {

		TestBase.clearDatabase(driver, schemaDatabase);

		MigrationsLock lock1 = new MigrationsLock(new Migrations.DefaultMigrationContext(MigrationsConfig.builder()
			.withDatabase(database1)
			.withSchemaDatabase(schemaDatabase)
			.build(), driver));
		lock1.lock();
		try {
			MigrationsLock lock2 = new MigrationsLock(new Migrations.DefaultMigrationContext(MigrationsConfig
				.builder()
				.withDatabase(database2)
				.withSchemaDatabase(schemaDatabase == null && !"neo4j".equalsIgnoreCase(database2) ? "neo4j" : schemaDatabase)
				.build(), driver));
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(lock2::lock).withMessage(
				"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed");
		} finally {
			lock1.unlock();
		}
	}

	@ParameterizedTest
	@CsvSource(nullValues = "n/a", value = {
		// All databases to be migrated are none existent, as we want to make sure the schema as well as the lock doesn't go there
		"n/a, b, n/a",
		"n/a, b, neo4j",
		"n/a, b, schemaDatabase",
		"a, b, schemaDatabase"
	})
	void lockShouldNotFailInSameDatabaseForDifferentDatabases(String database1, String database2, String schemaDatabase) {

		TestBase.clearDatabase(driver, schemaDatabase);

		MigrationsLock lock1 = new MigrationsLock(new Migrations.DefaultMigrationContext(MigrationsConfig.builder()
			.withDatabase(database1)
			.withSchemaDatabase(schemaDatabase)
			.build(), driver));
		lock1.lock();
		try {
			MigrationsLock lock2 = new MigrationsLock(new Migrations.DefaultMigrationContext(MigrationsConfig
				.builder()
				.withDatabase(database2)
				.withSchemaDatabase(schemaDatabase == null && !"neo4j".equalsIgnoreCase(database2) ? "neo4j" : schemaDatabase)
				.build(), driver));
			try {
				lock2.lock();
				try (Session session = driver.session(TestBase.getSessionConfig(schemaDatabase))) {
					List<String> namedLocks = session.run("MATCH (n:`__Neo4jMigrationsLock`) RETURN n.name")
						.list(r -> r.get(0).asString());
					assertThat(namedLocks).containsExactlyInAnyOrder(database1 == null ? "John Doe" : database1.toLowerCase(
						Locale.ROOT), database2.toLowerCase(Locale.ROOT));
				}
			} finally {
				lock2.unlock();
			}
		} finally {
			lock1.unlock();
		}
	}
}
