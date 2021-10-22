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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Discoverer of migrations.
 *
 * @author Michael J. Simons
 * @soundtrack Motörhead - 1916
 * @since 0.0.3
 */
interface Discoverer {

	/**
	 * Discover migrations within the given context.
	 *
	 * @param context The context of the ongoing migration.
	 * @return A collection of migrations.
	 */
	Collection<Migration> discoverMigrations(MigrationContext context);

	/**
	 * Discovers all Java based migrations.
	 */
	class JavaBasedMigrationDiscoverer implements Discoverer {

		@Override
		public Collection<Migration> discoverMigrations(MigrationContext context) {

			MigrationsConfig config = context.getConfig();
			if (config.getPackagesToScan().length == 0) {
				return Collections.emptyList();
			}

			try (ScanResult scanResult = new ClassGraph()
				.enableAllInfo()
				.acceptPackages(config.getPackagesToScan())
				.enableExternalClasses()
				.scan()) {

				return scanResult
					.getClassesImplementing(JavaBasedMigration.class.getName()).loadClasses(Migration.class)
					.stream()
					.map(c -> {
						try {
							return getConstructor(c).newInstance();
						} catch (Exception e) {
							throw new MigrationsException("Could not instantiate migration " + c.getName(), e);
						}
					}).collect(Collectors.toList());
			}
		}

		@SuppressWarnings("squid:S3011") // Very much the point of the whole thing
		private static Constructor<Migration> getConstructor(Class<Migration> c) throws NoSuchMethodException {
			Constructor<Migration> ctr = c.getDeclaredConstructor();
			ctr.setAccessible(true);
			return ctr;
		}
	}

	class CypherBasedMigrationDiscoverer implements Discoverer {

		private static final Logger LOGGER = Logger.getLogger(CypherBasedMigrationDiscoverer.class.getName());

		/**
		 * @return All Cypher based migrations. Empty list if no package to scan is configured.
		 */
		public Collection<Migration> discoverMigrations(MigrationContext context) {

			MigrationsConfig config = context.getConfig();
			List<Migration> listOfMigrations = new ArrayList<>();

			List<String> classpathLocations = new ArrayList<>();
			List<String> filesystemLocations = new ArrayList<>();

			for (String prefixAndLocation : config.getLocationsToScan()) {

				Location location = Location.of(prefixAndLocation);
				if (location.getType() == Location.LocationType.CLASSPATH) {
					classpathLocations.add(location.getName());
				} else if (location.getType() == Location.LocationType.FILESYSTEM) {
					filesystemLocations.add(location.getName());
				}
			}

			listOfMigrations.addAll(scanClasspathLocations(classpathLocations, context.getConfig()));
			listOfMigrations.addAll(scanFilesystemLocations(filesystemLocations, context.getConfig()));

			return listOfMigrations;
		}

		private List<Migration> scanClasspathLocations(List<String> classpathLocations, MigrationsConfig config) {

			if (classpathLocations.isEmpty()) {
				return Collections.emptyList();
			}

			LOGGER.log(Level.FINE, "Scanning for classpath resources in {0}", classpathLocations);

			String[] paths = classpathLocations.toArray(new String[0]);
			try (ScanResult scanResult = new ClassGraph().acceptPaths(paths).scan()) {

				return scanResult.getResourcesWithExtension(Defaults.CYPHER_SCRIPT_EXTENSION)
					.stream()
					.map(resource -> new CypherBasedMigration(resource.getURL(), config.isAutocrlf()))
					.collect(Collectors.toList());
			}
		}

		private List<Migration> scanFilesystemLocations(List<String> filesystemLocations, MigrationsConfig config) {

			if (filesystemLocations.isEmpty()) {
				return Collections.emptyList();
			}

			LOGGER.log(Level.FINE, "Scanning for filesystem resources in {0}", filesystemLocations);

			List<Migration> migrations = new ArrayList<>();

			for (String location : filesystemLocations) {
				Path path = Paths.get(location);
				try {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (attrs.isRegularFile() && file.getFileName().toString()
								.endsWith("." + Defaults.CYPHER_SCRIPT_EXTENSION)) {
								migrations.add(new CypherBasedMigration(file.toFile().toURI().toURL(), config.isAutocrlf()));
								return FileVisitResult.CONTINUE;
							}
							return super.visitFile(file, attrs);
						}
					});
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			return migrations;
		}
	}
}
