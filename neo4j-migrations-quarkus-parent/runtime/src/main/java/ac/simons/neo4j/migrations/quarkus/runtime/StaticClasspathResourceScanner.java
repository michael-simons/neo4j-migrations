/*
 * Copyright 2020-2022 the original author or authors.
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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.ClasspathResourceScanner;
import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.internal.Location;
import io.github.classgraph.ClassGraph;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;

/**
 * This resource scanner holds a list of {@link ResourceWrapper resource wrappers}, pointing to (Cypher) resources found
 * on the classpath. The resources as well as this special scanner are mutable. They must be as that is the way how
 * Quarkus works when serializing them to bytecode.
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
public final class StaticClasspathResourceScanner implements ClasspathResourceScanner {

	/**
	 * Creates a new scanner from a list of locations to scan. Non-classpath locations are filtered out
	 *
	 * @param locationsToScan The list of locations to scan
	 * @return a correctly initialized scanner
	 */
	public static StaticClasspathResourceScanner from(List<String> locationsToScan) {
		var paths = locationsToScan
			.stream()
			.map(Location::of)
			.filter(l -> l.getType() == Location.LocationType.CLASSPATH)
			.map(Location::getName)
			.toArray(String[]::new);

		try (var scanResult = new ClassGraph().acceptPaths(paths).scan()) {
			var resources = scanResult.getResourcesWithExtension(Defaults.CYPHER_SCRIPT_EXTENSION).stream()
				.map(r -> {
					var resource = new ResourceWrapper();
					resource.setUrl(r.getURI().toString());
					resource.setPath(r.getPath());
					return resource;
				})
				.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
			var scanner = new StaticClasspathResourceScanner();
			scanner.setResources(resources);
			return scanner;
		}
	}

	private Set<ResourceWrapper> resources = Set.of();

	/**
	 * @return the set of scanned / discovered resources
	 */
	public Set<ResourceWrapper> getResources() {
		return resources;
	}

	/**
	 * This method may not be used outside Quarkus internal code.
	 *
	 * @param resources a new set of resources
	 */
	public void setResources(Set<ResourceWrapper> resources) {
		this.resources = resources;
	}

	/**
	 * This method takes the statically found URLs and transform them depending on the runtime: If the scan is invoked
	 * in native image runtime, the {@literal resource:} protocol is used with the original path of the resource. Otherwise
	 * the current thread classloader is used to retrieve a resource url with that path. If that url is null, we are most
	 * likely looking at {@literal file:} url.
	 * <p>
	 * The URLs must be recreated for reasons explained in {@link ResourceWrapper#getUrl()}.
	 *
	 * @param locations The locations to scan
	 * @return The resources found
	 * @see ClasspathResourceScanner#scan(List)
	 */
	@Override
	public List<URL> scan(List<String> locations) {

		var ccl = Thread.currentThread().getContextClassLoader();
		var stripped = locations.stream()
			.map(location -> location.charAt(0) == '/' ? location.substring(1) : location).collect(Collectors.toList());
		return resources.stream()
			.filter(r -> stripped.stream().anyMatch(location -> r.getPath().startsWith(location)))
			.map(rw -> {
				try {
					if (ImageInfo.inImageRuntimeCode()) {
						return new URL("resource:" + rw.getPath());
					} else {
						var classpathURL = ccl.getResource(rw.getPath());
						// The classpath URL is most likely null when the thing is only accessible via file protocol
						return classpathURL == null ? new URL(rw.getUrl()) : classpathURL;
					}
				} catch (MalformedURLException e) {
					throw new MigrationsException("Could recreate an URL from a resource wrapper.", e);
				}
			})
			.collect(Collectors.toList());
	}
}
