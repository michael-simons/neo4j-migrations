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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 * Factory providing different {@link Discoverer} implementations.
 *
 * @author Michael J. Simons
 * @param <T> The concrete type to be instantiated with a discovered resource
 * @since 1.2.2
 */
final class ResourceDiscoverer<T> implements Discoverer<T> {

	static Discoverer<Migration> forMigrations(ClasspathResourceScanner resourceScanner) {

		List<Discoverer<Migration>> allDiscovers = new ArrayList<>();
		for (ResourceBasedMigrationProvider provider : ResourceBasedMigrationProvider.unique()) {
			Predicate<String> filter = pathOrUrl -> {
				String path = URLDecoder.decode(pathOrUrl, Defaults.CYPHER_SCRIPT_ENCODING);
				if (provider.supportsArbitraryResourceNames()) {
					return path.endsWith(provider.getExtension());
				}
				Matcher matcher = MigrationVersion.VERSION_PATTERN.matcher(path);
				boolean isValidResource = matcher.find();
				if (!isValidResource && LOGGER.isLoggable(Level.FINE) && !LifecyclePhase.canParse(path)) {
					LOGGER.log(Level.FINE, "Skipping resource {0}", path);
				}
				return isValidResource && provider.getExtension().equals(matcher.group("ext"));
			};
			allDiscovers.add(new ResourceDiscoverer<>(resourceScanner, filter, provider::handle));
		}
		return new AggregatingMigrationDiscoverer(allDiscovers);
	}

	static ResourceDiscoverer<Callback> forCallbacks(ClasspathResourceScanner resourceScanner) {
		Predicate<String> filter = LifecyclePhase::canParse;
		filter = filter.and(fullPath -> {
				final int lastSlashIdx = fullPath.lastIndexOf('/');
				final int lastDotIdx = fullPath.lastIndexOf('.');
				return lastDotIdx > lastSlashIdx && fullPath.substring(lastDotIdx + 1).equalsIgnoreCase(Defaults.CYPHER_SCRIPT_EXTENSION);
		});
		return new ResourceDiscoverer<>(resourceScanner, filter,
			ctx -> Collections.singletonList(new CypherBasedCallback(ctx)));
	}

	private final ClasspathResourceScanner scanner;

	private final Predicate<String> resourceFilter;

	private final Function<ResourceContext, Collection<T>> mapper;

	private ResourceDiscoverer(ClasspathResourceScanner scanner, Predicate<String> resourceFilter, Function<ResourceContext, Collection<T>> mapper) {
		this.scanner = scanner;
		this.resourceFilter = resourceFilter;
		this.mapper = mapper;
	}

	/**
	 * @return All Cypher-based migrations. Empty list if no package to scan is configured.
	 */
	@Override
	public Collection<T> discover(MigrationContext context) {

		MigrationsConfig config = context.getConfig();
		List<T> listOfMigrations = new ArrayList<>();

		List<String> classpathLocations = new ArrayList<>();
		List<URI> filesystemLocations = new ArrayList<>();

		for (String prefixAndLocation : config.getLocationsToScan()) {

			Location location = Location.of(prefixAndLocation);
			if (location.getType() == Location.LocationType.CLASSPATH) {
				classpathLocations.add(location.getName());
			} else if (location.getType() == Location.LocationType.FILESYSTEM) {
				filesystemLocations.add(location.toUri());
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

		return this.scanner.scan(classpathLocations)
			.stream()
			.filter(r -> resourceFilter.test(r.getPath()))
			.map(resource -> ResourceContext.of(resource, config))
			.map(mapper)
			.flatMap(Collection::stream)
			.toList();
	}

	private List<T> scanFilesystemLocations(List<URI> filesystemLocations, MigrationsConfig config) {

		if (filesystemLocations.isEmpty()) {
			return Collections.emptyList();
		}

		LOGGER.log(Level.FINE, "Scanning for filesystem resources in {0}", filesystemLocations);

		List<T> resources = new ArrayList<>();

		for (URI location : filesystemLocations) {
			Path path = Paths.get(location);
			if (!Files.isDirectory(path)) {
				LOGGER.log(Level.WARNING, "Ignoring `{0}` (not a directory)", path);
				continue;
			}
			try {
				Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String fullPath = file.toString();
						if (attrs.isRegularFile() && resourceFilter.test(fullPath)) {
							ResourceContext context = ResourceContext.of(file.toFile().toURI().toURL(), config);
							resources.addAll(mapper.apply(context));
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
