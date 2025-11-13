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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A location for migration scripts, can be either on the classpath or the filesystem.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
public final class Location {

	private static final Set<Path> DOTS = Set.of(Path.of("."), Path.of(".."));

	private final LocationType type;

	private final String name;

	private Location(LocationType type, String name) {
		this.type = type;
		this.name = (name.startsWith("/") ? name : "/" + name);
	}

	private static Optional<Path> tryRelative(String uri) {
		var path = Path.of(uri);
		if (path.getNameCount() >= 1 && !path.isAbsolute() && DOTS.contains(path.getName(0))) {
			return Optional.of(resolveAndNormalize(path));
		}
		return Optional.empty();
	}

	private static Path resolveAndNormalize(Path path) {
		return Path.of("").toAbsolutePath().resolve(path).normalize();
	}

	/**
	 * Creates a new {@link Location} object from a given location that has an optional
	 * prefix (protocol) and a name.
	 * @param uri a name with an optional scheme / name
	 * @return a location object
	 */
	public static Location of(String uri) {
		// Yep, Regex would work, too. I know how to do this with regex, but it's slower
		// and not necessary.
		int indexOfFirstColon = uri.indexOf(":");
		if (indexOfFirstColon < 0) {
			return tryRelative(uri).map(path -> new Location(LocationType.FILESYSTEM, path.toString()))
				.orElseGet(() -> new Location(LocationType.CLASSPATH, uri));
		}

		String prefix = uri.substring(0, indexOfFirstColon).toLowerCase(Locale.ENGLISH).trim();
		String name = uri.substring(indexOfFirstColon + 1).trim().replace('\\', '/');

		if (name.isEmpty()) {
			throw new MigrationsException("Invalid location: '" + uri + "'");
		}

		LocationType type;
		if (LocationType.CLASSPATH.getPrefix().equals(prefix)) {
			type = LocationType.CLASSPATH;
		}
		else if (LocationType.FILESYSTEM.getPrefix().equals(prefix)) {
			type = LocationType.FILESYSTEM;
			try {
				// Let's delegate to Java if we can get a valid path out of this URL, this
				// is all madness
				// https://en.wikipedia.org/wiki/File_URI_scheme
				// If we encounter a non-empty authority, we strip it away and try again,
				// the relative paths
				// handling follows later. As the first / will always denote root,
				// resolving will never resolve
				// against the current working directory.
				var hlp = URI.create(String.format("%s:%s", prefix, name));
				if (hlp.getAuthority() != null) {
					hlp = URI.create(String.format("%s:%s", prefix, hlp.getPath()));
				}
				name = Paths.get(hlp).toString();
			}
			catch (IllegalArgumentException ex) {
				name = null;
			}
		}
		else {
			String supportedSchemes = Arrays.stream(LocationType.values())
				.map(LocationType::getPrefix)
				.map(s -> String.format("'%s:'", s))
				.collect(Collectors.joining(", "));
			throw new MigrationsException(
					"Invalid scheme: '" + prefix + "', supported schemes are " + supportedSchemes);
		}

		if (name == null || name.isEmpty()) {
			throw new MigrationsException(
					"Invalid path; a valid file location must begin with either file:/path (no hostname), file:///path (empty hostname), or file://hostname/path");
		}

		if (type == LocationType.FILESYSTEM) {
			name = resolveAndNormalize(Path.of(name)).toString();
		}

		return new Location(type, name);
	}

	/**
	 * {@return the type of this location}
	 */
	public LocationType getType() {
		return this.type;
	}

	/**
	 * {@return the name of this location}
	 */
	public String getName() {
		return this.name;
	}

	URI toUri() {
		try {
			return new URI(this.type.getPrefix(), "", this.name, null);
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	/**
	 * A location type. If no prefix is given, we assume {@literal classpath:}.
	 */
	public enum LocationType {

		/**
		 * A location inside the classpath.
		 */
		CLASSPATH("classpath"),
		/**
		 * A location inside the filesystem.
		 */
		FILESYSTEM("file");

		private final String prefix;

		LocationType(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * {@return the prefix / protocol under which the location is recognized}
		 */
		public String getPrefix() {
			return this.prefix;
		}

	}

}
