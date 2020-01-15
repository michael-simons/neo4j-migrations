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

import ac.simons.neo4j.migrations.core.MigrationVersion.VersionComparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;

/**
 * Main entry to Neo4j Migrations
 *
 * @author Michael J. Simons
 */
public final class Migrations {

	private static final Logger LOGGER = Logger.getLogger(Migrations.class.getName());

	private final MigrationsConfig config;
	private final Driver driver;
	private final MigrationContext context;
	private final DiscoveryService discoveryService = new DiscoveryService();

	public Migrations(MigrationsConfig config, Driver driver) {

		this.config = config;
		this.driver = driver;
		this.context = new DefaultMigrationContext(this.config, this.driver);
	}

	public void apply() {

		MigrationsLock lock = new MigrationsLock(this.context);
		try {
			lock.lock();
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			apply0(migrations);
		} finally {
			lock.unlock();
		}
	}

	private Optional<MigrationVersion> getLastAppliedVersion() {

		try (Session session = driver.session(context.getSessionConfig())) {
			String versionValue = session.run(
				"MATCH (l:__Neo4jMigration) WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) RETURN l.version AS version")
				.single().get("version").asString();

			return Optional.of(MigrationVersion.withValue(versionValue));
		} catch (NoSuchRecordException e) {
			return Optional.empty();
		}
	}

	private Map<MigrationVersion, Optional<String>> getChainOfAppliedMigrations() {

		Map<MigrationVersion, Optional<String>> chain = new LinkedHashMap<>();
		try (Session session = driver.session(context.getSessionConfig())) {
			Record r = session
				.run("MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:__Neo4jMigration) \n"
					+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration)\n"
					+ "RETURN p").single();
			r.get("p").asPath().nodes().forEach(migration -> {
				chain.put(MigrationVersion.withValue(migration.get("version").asString()),
					Optional.ofNullable(migration.get("checksum").asString(null)));
			});
		}
		return chain;
	}

	/**
	 * @param newMigrations A list sorted by {@link Migration#getVersion()}.
	 */
	private void verifyChain(List<Migration> newMigrations) {

		Map<MigrationVersion, Optional<String>> chain = getChainOfAppliedMigrations();

		int i = 0;
		for (Map.Entry<MigrationVersion, Optional<String>> entry : chain.entrySet()) {
			MigrationVersion expectedVersion = entry.getKey();
			Optional<String> expectedChecksum = entry.getValue();

			// Skip base line
			if (expectedVersion == MigrationVersion.baseline()) {
				continue;
			}

			Migration newMigration = newMigrations.get(i);
			if (!newMigration.getVersion().equals(expectedVersion)) {
				throw new MigrationsException("Unexpected migration at index " + i + ": " + toString(newMigration));
			}

			if (!expectedChecksum.equals(newMigration.getChecksum())) {
				throw new MigrationsException(("Checksum of " + toString(newMigration) + " changed!"));
			}
			++i;
		}
	}

	private void apply0(List<Migration> migrations) {

		MigrationVersion previousVersion = getLastAppliedVersion()
			.orElseGet(() -> MigrationVersion.baseline());

		if (previousVersion != MigrationVersion.baseline()) {
			verifyChain(migrations);
		}

		VersionComparator comparator = new VersionComparator();
		StopWatch stopWatch = new StopWatch();
		for (Migration migration : migrations) {

			if (previousVersion != MigrationVersion.baseline()
				&& comparator.compare(migration.getVersion(), previousVersion) <= 0) {
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

		try (Session session = driver.session(context.getSessionConfig())) {
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
						+ "MERGE (p) - [:MIGRATED_TO {at: datetime(), in: $executionTime, by: $osUser, connectedAs: neo4jUser}] -> (c)",
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
		properties.put("type", migration.getType().getValue().name());
		properties.put("source", migration.getSource());
		migration.getChecksum().ifPresent(checksum -> properties.put("checksum", checksum));

		return Collections.unmodifiableMap(properties);
	}

	private static String toString(Migration migration) {

		return String.format("%s (\"%s\")", migration.getVersion(), migration.getDescription());
	}
}
