/*
 * Copyright 2020-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @since 0.0.2
 */
public final class Location {

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
		 * @return the prefix / protocol under which the location is recognized
		 */
		public String getPrefix() {
			return prefix;
		}
	}

	/**
	 * Creates a new {@link Location} object from a given location that has an optional prefix (protocol) and a name
	 * @param uri A name with an optional scheme / name
	 * @return A location object
	 */
	public static Location of(String uri) {
		// Yep, Regex would work, too. I know how to do this with regex, but it's slower and not necessary.
		int indexOfFirstColon = uri.indexOf(":");
		if (indexOfFirstColon < 0) {
			return new Location(LocationType.CLASSPATH, uri);
		}

		String prefix = uri.substring(0, indexOfFirstColon).toLowerCase(Locale.ENGLISH).trim();
		String name = uri.substring(indexOfFirstColon + 1).trim().replace('\\', '/');

		if (name.isEmpty()) {
			throw new MigrationsException("Invalid location: '" + uri + "'");
		}

		LocationType type;
		if (LocationType.CLASSPATH.getPrefix().equals(prefix)) {
			type = LocationType.CLASSPATH;
		} else if (LocationType.FILESYSTEM.getPrefix().equals(prefix)) {
			type = LocationType.FILESYSTEM;
			name = URI.create(String.format("%s:%s", prefix, name)).getPath();
		} else {
			String supportedSchemes = Arrays.stream(LocationType.values()).map(LocationType::getPrefix)
				.map(s -> String.format("'%s:'", s))
				.collect(Collectors.joining(", "));
			throw new MigrationsException("Invalid scheme: '" + prefix + "', supported schemes are " + supportedSchemes);
		}

		if (name == null || name.length() == 0) {
			throw new MigrationsException("Invalid path; a valid file location must begin with either file:/path (no hostname), file:///path (empty hostname), or file://hostname/path");
		}

		return new Location(type, name);
	}

	private final LocationType type;
	private final String name;

	private Location(LocationType type, String name) {
		this.type = type;
		this.name = (name.startsWith("/") ? name : "/" + name);
	}

	/**
	 * @return the type of this location
	 */
	public LocationType getType() {
		return type;
	}

	/**
	 * @return the name of this location
	 */
	public String getName() {
		return name;
	}

	URI toUri() {
		try {
			return new URI(type.getPrefix(), "", name, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}
