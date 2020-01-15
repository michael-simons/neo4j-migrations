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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;

/**
 * Main entry to Neo4j Migrations
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class Migrations {

	private static final Logger LOGGER = Logger.getLogger(Migrations.class.getName());

	private final MigrationsConfig config;
	private final Driver driver;
	private final MigrationContext context;
	private final DiscoveryService discoveryService;
	private final ChainBuilder chainBuilder;

	public Migrations(MigrationsConfig config, Driver driver) {

		this.config = config;
		this.driver = driver;

		this.discoveryService = new DiscoveryService();
		this.chainBuilder = new ChainBuilder();

		this.context = new DefaultMigrationContext(this.config, this.driver);
	}

	/**
	 * Returns information about the context, the database, all applied and all pending applications.
	 *
	 * @return The chain of migrations.
	 * @since 0.0.4
	 */
	public MigrationChain info() {

		return executeWithinLock(() -> {
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			return chainBuilder.buildChain(context, migrations);
		});
	}

	/**
	 * Applies a all discovered Neo4j migrations. Migrations can either be classes implementing {@link JavaBasedMigration}
	 * or Cypher script migrations that are on the classpath or filesystem.
	 *
	 * @since 0.0.1
	 */
	public void apply() {

		executeWithinLock(() -> {
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			apply0(migrations);
		});
	}

	private void executeWithinLock(Runnable executable) {

		executeWithinLock(() -> {
			executable.run();
			return null;
		});
	}

	private <T> T executeWithinLock(Supplier<T> executable) {

		MigrationsLock lock = new MigrationsLock(this.context);
		try {
			lock.lock();
			return executable.get();
		} finally {
			lock.unlock();
		}
	}

	private Optional<MigrationVersion> getLastAppliedVersion() {

		try (Session session = context.getSession()) {
			String versionValue = session.run(
				"MATCH (l:__Neo4jMigration) WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) RETURN l.version AS version")
				.single().get("version").asString();

			return Optional.of(MigrationVersion.withValue(versionValue));
		} catch (NoSuchRecordException e) {
			return Optional.empty();
		}
	}

	private void apply0(List<Migration> migrations) {

		MigrationVersion previousVersion = getLastAppliedVersion()
			.orElseGet(() -> MigrationVersion.baseline());

		// Validate and build the chain of migrations
		MigrationChain chain = chainBuilder.buildChain(context, migrations);

		StopWatch stopWatch = new StopWatch();
		for (Migration migration : migrations) {

			if (previousVersion != MigrationVersion.baseline() && chain.isApplied(migration.getVersion().getValue())) {
				LOGGER.log(Level.INFO, "Skipping already applied migration {0}", toString(migration));
				continue;
			}
			try {
				stopWatch.start();
				migration.apply(context);
				long executionTime = stopWatch.stop();
				previousVersion = recordApplication(previousVersion, migration, executionTime);

				LOGGER.log(Level.INFO, "Applied migration {0}", toString(migration));
			} catch (Exception e) {
				throw new MigrationsException("Could not apply migration: " + toString(migration), e);
			} finally {
				stopWatch.reset();
			}
		}
	}

	private MigrationVersion recordApplication(MigrationVersion previousVersion, Migration appliedMigration,
		long executionTime) {

		try (Session session = context.getSession()) {
			session.writeTransaction(t -> {
				Value parameters = Values.parameters(
					"previousVersion", previousVersion.getValue(),
					"appliedMigration", toProperties(appliedMigration),
					"osUser", System.getProperty("user.name"),
					"executionTime", executionTime
				);

				return t.run(""
						+ "CALL dbms.showCurrentUser() YIELD username AS neo4jUser "
						+ "WITH neo4jUser "
						+ "MERGE (p:__Neo4jMigration {version: $previousVersion}) "
						+ "CREATE (c:__Neo4jMigration) SET c = $appliedMigration "
						+ "MERGE (p) - [:MIGRATED_TO {at: datetime({timezone: 'UTC'}), in: duration( {milliseconds: $executionTime} ), by: $osUser, connectedAs: neo4jUser}] -> (c)",
					parameters)
						.consume();
				}
			);
		}

		return appliedMigration.getVersion();
	}

	private static Map<String, Object> toProperties(Migration migration) {

		Map<String, Object> properties = new HashMap<>();

		properties.put("version", migration.getVersion().getValue());
		properties.put("description", migration.getDescription());
		properties.put("type", getMigrationType(migration).name());
		properties.put("source", migration.getSource());
		migration.getChecksum().ifPresent(checksum -> properties.put("checksum", checksum));

		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Returns the type of a migration. It's not part of the API so that it is not possible to be overwritten by
	 * classes implementing {@link JavaBasedMigration}.
	 *
	 * @param migration The migration who's type should be computed
	 * @return The type of the migration.
	 */
	static MigrationType getMigrationType(Migration migration) {

		MigrationType type;
		if (migration instanceof JavaBasedMigration) {
			type = MigrationType.JAVA;
		} else if (migration instanceof CypherBasedMigration) {
			type = MigrationType.CYPHER;
		} else {
			throw new MigrationsException("Unknown migration type: " + migration.getClass());
		}
		return type;
	}

	static String toString(Migration migration) {

		return String.format("%s (\"%s\")", migration.getVersion(), migration.getDescription());
	}

	static class DefaultMigrationContext implements MigrationContext {

		private final MigrationsConfig config;

		private final Driver driver;

		private final SessionConfig sessionConfig;

		DefaultMigrationContext(MigrationsConfig config, Driver driver) {
			this.config = config;
			this.driver = driver;

			SessionConfig.Builder sessionConfigBuilder = SessionConfig.builder()
				.withDefaultAccessMode(AccessMode.WRITE);
			if (!(this.config.getDatabase() == null || this.config.getDatabase().trim().isEmpty())) {
				sessionConfigBuilder.withDatabase(this.config.getDatabase().trim());
			}
			this.sessionConfig = sessionConfigBuilder.build();
		}

		@Override
		public MigrationsConfig getConfig() {
			return config;
		}

		@Override
		public Driver getDriver() {
			return driver;
		}

		@Override
		public SessionConfig getSessionConfig() {
			return sessionConfig;
		}
	}
}
