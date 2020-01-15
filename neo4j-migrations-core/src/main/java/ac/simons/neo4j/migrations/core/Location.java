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

import java.util.Locale;

/**
 * @author Michael J. Simons
 * @since 0.0.2
 */
final class Location {

	enum LocationType {

		CLASSPATH,
		FILESYSTEM
	}

	static Location of(String prefixAndName) {
		// Yep, Regex would work, too. I know how to do this with regex, but it's slower and not necessary.
		int indexOfFirstColon = prefixAndName.indexOf(":");
		if (indexOfFirstColon < 0) {
			return new Location(LocationType.CLASSPATH, prefixAndName);
		}
		if (prefixAndName.length() < 3) {
			throw new MigrationsException("Invalid resource name: '" + prefixAndName + "'");
		}

		String prefix = prefixAndName.substring(0, indexOfFirstColon).toLowerCase(Locale.ENGLISH).trim();
		String name = prefixAndName.substring(indexOfFirstColon + 1).trim();

		if (name.length() == 0) {
			throw new MigrationsException("Invalid name.");
		}
		switch (prefix) {
			case MigrationsConfig.PREFIX_CLASSPATH:
				return new Location(LocationType.CLASSPATH, name);
			case MigrationsConfig.PREFIX_FILESYSTEM:
				return new Location(LocationType.FILESYSTEM, name);
			default:
				throw new MigrationsException(("Invalid resource prefix: '" + prefix + "'"));
		}
	}

	private final LocationType type;
	private final String name;

	private Location(LocationType type, String name) {
		this.type = type;
		this.name = name;
	}

	public LocationType getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
