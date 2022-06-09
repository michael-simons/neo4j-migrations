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
package ac.simons.neo4j.migrations.core;

import java.net.URI;
import java.util.Locale;

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
		if (uri.length() < 3) {
			throw new MigrationsException("Invalid resource name: '" + uri + "'");
		}

		String prefix = uri.substring(0, indexOfFirstColon).toLowerCase(Locale.ENGLISH).trim();
		String name = uri.substring(indexOfFirstColon + 1).trim();

		if (name.length() == 0) {
			throw new MigrationsException("Invalid name.");
		}

		LocationType type;
		if (LocationType.CLASSPATH.getPrefix().equals(prefix)) {
			type = LocationType.CLASSPATH;
		} else if (LocationType.FILESYSTEM.getPrefix().equals(prefix)) {
			type = LocationType.FILESYSTEM;
		} else {
			throw new MigrationsException("Invalid resource prefix: '" + prefix + "'");
		}

		return new Location(type, name);
	}

	private final LocationType type;
	private final String name;

	private Location(LocationType type, String name) {
		this.type = type;
		this.name = name;
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
		return URI.create(type.getPrefix() + ":" + name.replace('\\', '/'));
	}
}
