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

import ac.simons.neo4j.migrations.Location.LocationType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

	public Migrations(MigrationsConfig config, Driver driver) {

		this.config = config;
		this.driver = driver;
	}

	public void apply() {

		MigrationsLock lock = new MigrationsLock(driver);
		try {
			lock.lock();
			List<Migration> migrations = findMigrations();
			apply0(migrations);
		} finally {
			lock.unlock();
		}
	}

	private Optional<MigrationVersion> getLastAppliedVersion() {

		try (Session session = driver.session()) {
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
		try (Session session = driver.session()) {
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
				throw new MigrationsException(
					"Unexpected migration at index " + i + ": " + toString(newMigration));
			}

			if (!expectedChecksum.equals(newMigration.getChecksum())) {
				throw new MigrationsException(("Checksum of " + toString(newMigration) + " changed!"));
			}
			++i;
		}
	}

	private void apply0(List<Migration> migrations) {

		// Build context
		MigrationContext context = new DefaultMigrationContext(this.config, this.driver);

		MigrationVersion previousVersion = getLastAppliedVersion()
			.orElseGet(() -> MigrationVersion.baseline());

		if (previousVersion != MigrationVersion.baseline()) {
			verifyChain(migrations);
		}

		VersionComparator comparator = new VersionComparator();
		for (Migration migration : migrations) {

			if (previousVersion != MigrationVersion.baseline()
				&& comparator.compare(migration.getVersion(), previousVersion) <= 0) {
				LOGGER.log(Level.INFO, "Skipping already applied migration {0}", toString(migration));
				continue;
			}
			try {
				migration.apply(context);
				previousVersion = recordApplication(previousVersion, migration);

				LOGGER.log(Level.INFO, "Applied migration {0}", toString(migration));
			} catch (Exception e) {
				throw new MigrationsException("Could not apply migration: " + toString(migration), e);
			}
		}
	}

	private MigrationVersion recordApplication(MigrationVersion previousVersion, Migration appliedMigration) {

		try (Session session = driver.session()) {
			session.writeTransaction(t ->
				{
					Value parameters = Values.parameters(
						"previousVersion", previousVersion.getValue(),
						"appliedMigration", toProperties(appliedMigration),
						"osUser", System.getProperty("user.name")
					);
					return t.run(""
						+ "CALL dbms.showCurrentUser() YIELD username AS neo4jUser "
						+ "WITH neo4jUser "
						+ "MERGE (p:__Neo4jMigration {version: $previousVersion}) "
						+ "CREATE (c:__Neo4jMigration) SET c = $appliedMigration "
						+ "MERGE (p) - [:MIGRATED_TO {at: datetime(), by: $osUser, connectedAs: neo4jUser}] -> (c)", parameters)
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

	/**
	 * @return An unmodifiable list of migrations sorted by version.
	 */
	List<Migration> findMigrations() {

		List<Migration> migrations = new ArrayList<>();
		try {
			migrations.addAll(findJavaBasedMigrations());
			migrations.addAll(findCypherBasedMigrations());
		} catch (IOException e) {
			throw new MigrationsException("Unexpected error while scanning for migrations", e);
		}

		Collections.sort(migrations, Comparator.comparing(Migration::getVersion, new VersionComparator()));
		return Collections.unmodifiableList(migrations);
	}

	/**
	 * @return All Java based migrations. Empty list if no package to scan is configured.
	 */
	private List<Migration> findJavaBasedMigrations() {

		if (config.getPackagesToScan().length == 0) {
			return Collections.emptyList();
		}

		try (ScanResult scanResult = new ClassGraph()
			.enableAllInfo()
			.whitelistPackages(config.getPackagesToScan())
			.enableExternalClasses()
			.scan()) {

			return scanResult.getClassesImplementing(JavaBasedMigration.class.getName()).loadClasses()
				.stream()
				.map(c -> {
					try {
						Constructor<Migration> ctr = (Constructor<Migration>) c.getDeclaredConstructor();
						ctr.setAccessible(true);
						return ctr.newInstance();
					} catch (Exception e) {
						throw new MigrationsException("Could not instantiate migration " + c.getName(), e);
					}
				}).collect(Collectors.toList());
		}
	}

	/**
	 * @return All Cypher based migrations. Empty list if no package to scan is configured.
	 */
	private List<Migration> findCypherBasedMigrations() throws IOException {

		List<Migration> listOfMigrations = new ArrayList<>();

		List<String> classpathLocations = new ArrayList<>();
		List<String> filesystemLocations = new ArrayList<>();

		for (String prefixAndLocation : config.getLocationsToScan()) {

			Location location = Location.of(prefixAndLocation);
			if (location.getType() == LocationType.CLASSPATH) {
				classpathLocations.add(location.getName());
			} else if (location.getType() == LocationType.FILESYSTEM) {
				filesystemLocations.add(location.getName());
			}
		}

		listOfMigrations.addAll(scanClasspathLocations(classpathLocations));
		listOfMigrations.addAll(scanFilesystemLocations(filesystemLocations));

		return listOfMigrations;
	}

	private List<Migration> scanClasspathLocations(List<String> classpathLocations) {

		if (classpathLocations.isEmpty()) {
			return Collections.emptyList();
		}

		LOGGER.log(Level.INFO, "Scanning for classpath resources in {0}", classpathLocations);

		try (ScanResult scanResult = new ClassGraph()
			.whitelistPaths(classpathLocations.toArray(new String[classpathLocations.size()])).scan()) {

			return scanResult.getResourcesWithExtension(Defaults.CYPHER_SCRIPT_EXTENSION)
				.stream()
				.map(resource -> new CypherBasedMigration(resource.getURL()))
				.collect(Collectors.toList());
		}
	}

	private List<Migration> scanFilesystemLocations(List<String> filesystemLocations) throws IOException {

		if (filesystemLocations.isEmpty()) {
			return Collections.emptyList();
		}

		LOGGER.log(Level.INFO, "Scanning for filesystem resources in {0}", filesystemLocations);

		List<Migration> migrations = new ArrayList<>();

		for (String location : filesystemLocations) {
			Path path = Paths.get(location);
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && file.getFileName().toString()
						.endsWith("." + Defaults.CYPHER_SCRIPT_EXTENSION)) {
						migrations.add(new CypherBasedMigration(file.toFile().toURI().toURL()));
						return FileVisitResult.CONTINUE;
					}
					return super.visitFile(file, attrs);
				}
			});
		}

		return migrations;
	}

	private static class VersionComparator implements Comparator<MigrationVersion> {

		@Override
		public int compare(MigrationVersion o1, MigrationVersion o2) {
			if (o1 == MigrationVersion.baseline()) {
				return -1;
			}

			return o1.getValue().compareTo(o2.getValue());
		}
	}
}
