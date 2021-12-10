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
import java.net.URL;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract base class for implementing discoverer discovering Cypher resources.
 *
 * @author Michael J. Simons
 * @param <T> The concrete type to be instantiated with a discovered resource
 * @since 1.2.2
 */
final class CypherResourceDiscoverer<T> implements Discoverer<T> {

	static CypherResourceDiscoverer<Migration> forMigrations() {
		return new CypherResourceDiscoverer<>(MigrationVersion::canParse, ctx -> new CypherBasedMigration(ctx.url, ctx.config.isAutocrlf()));
	}

	static CypherResourceDiscoverer<Callback> forCallbacks() {
		return new CypherResourceDiscoverer<>(LifecyclePhase::canParse,
			ctx -> new CypherBasedCallback(ctx.url, ctx.config.isAutocrlf()));
	}

	static final class ResourceContext {
		final URL url;

		final MigrationsConfig config;

		ResourceContext(URL url, MigrationsConfig config) {
			this.url = url;
			this.config = config;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(CypherResourceDiscoverer.class.getName());

	private final Predicate<String> resourceFilter;

	private final Function<ResourceContext, T> mapper;

	private CypherResourceDiscoverer(Predicate<String> resourceFilter, Function<ResourceContext, T> mapper) {
		this.resourceFilter = resourceFilter;
		this.mapper = mapper;
	}

	/**
	 * @return All Cypher based migrations. Empty list if no package to scan is configured.
	 */
	@Override
	public Collection<T> discover(MigrationContext context) {

		MigrationsConfig config = context.getConfig();
		List<T> listOfMigrations = new ArrayList<>();

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

	private List<T> scanClasspathLocations(List<String> classpathLocations, MigrationsConfig config) {

		if (classpathLocations.isEmpty()) {
			return Collections.emptyList();
		}

		LOGGER.log(Level.FINE, "Scanning for classpath resources in {0}", classpathLocations);

		String[] paths = classpathLocations.toArray(new String[0]);
		try (ScanResult scanResult = new ClassGraph().acceptPaths(paths).scan()) {

			return scanResult.getResourcesWithExtension(Defaults.CYPHER_SCRIPT_EXTENSION)
					.stream()
					.filter(r -> resourceFilter.test(r.getPath()))
					.map(resource -> new ResourceContext(resource.getURL(), config))
					.map(mapper)
					.collect(Collectors.toList());
		}
	}

	private List<T> scanFilesystemLocations(List<String> filesystemLocations, MigrationsConfig config) {

		if (filesystemLocations.isEmpty()) {
			return Collections.emptyList();
		}

		LOGGER.log(Level.FINE, "Scanning for filesystem resources in {0}", filesystemLocations);

		List<T> resources = new ArrayList<>();

		Predicate<String> hasExtension = fullPath -> {
			final int lastSlashIdx = fullPath.lastIndexOf('/');
			final int lastDotIdx = fullPath.lastIndexOf('.');
			return lastDotIdx > lastSlashIdx  && fullPath.substring(lastDotIdx + 1).equalsIgnoreCase(Defaults.CYPHER_SCRIPT_EXTENSION);
		};

		for (String location : filesystemLocations) {
			Path path = Paths.get(location);
			if (!Files.isDirectory(path)) {
				continue;
			}
			try {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String fullPath = file.toString();
						if (attrs.isRegularFile() && hasExtension.test(fullPath) && resourceFilter.test(fullPath)) {
							ResourceContext context = new ResourceContext(file.toFile().toURI().toURL(), config);
							resources.add(mapper.apply(context));
							return FileVisitResult.CONTINUE;
						}
						return super.visitFile(file, attrs);
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return resources;
	}
}
