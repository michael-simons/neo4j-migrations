/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A migrations version.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class MigrationVersion {

	static final String VERSION_PATTERN_BASE = "(?<type>[VR])(?<version>\\d+(?:_\\d+)*+|\\d+(?:\\.\\d+)*+)";

	@SuppressWarnings("squid:S5843") // This is a fine regex
	static final Pattern VERSION_PATTERN = Pattern
		.compile(VERSION_PATTERN_BASE + "__(?<name>[\\w ]+)(?:\\.(?<ext>\\w+))?");
	static final Pattern STOP_VERSION_PATTERN = Pattern
		.compile(VERSION_PATTERN_BASE + "(?<op>\\?)?(__(?<name>[\\w ]+)(?:\\.(?<ext>\\w+))?)?");

	private static final String BASELINE_VALUE = "BASELINE";

	private static final MigrationVersion BASELINE = new MigrationVersion(BASELINE_VALUE, null, false);

	private final String value;

	private final String description;

	/**
	 * A flag indicating that this version can be safely repeated, even on checksum
	 * changes.
	 * @since 1.13.3
	 */
	private final boolean repeatable;

	private MigrationVersion(String value, String description, boolean repeatable) {
		this.value = value;
		this.description = description;
		this.repeatable = repeatable;
	}

	/**
	 * Returns {@literal true} when the given path or URL can be parsed into a valid
	 * {@link MigrationVersion}.
	 * @param pathOrUrl a string representing either a path or a URL.
	 * @return {@literal true} when the given path or URL can be parsed into a valid
	 * {@link MigrationVersion}
	 */
	public static boolean canParse(String pathOrUrl) {
		return VERSION_PATTERN.matcher(pathOrUrl).find();
	}

	static MigrationVersion of(Class<?> clazz) {
		return parse(clazz.getSimpleName());
	}

	/**
	 * Creates a {@link MigrationVersion} from the given class or file name.
	 * @param name a class or file name
	 * @return a {@link MigrationVersion}
	 * @throws MigrationsException if the name cannot be parsed. You might check
	 * {{@link #canParse(String)}} prior to using this method
	 * @since 2.2.0
	 */
	public static MigrationVersion parse(String name) {

		Matcher matcher = VERSION_PATTERN.matcher(name);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid class name for a migration: " + name);
		}

		boolean repeatable = "R".equalsIgnoreCase(matcher.group("type"));
		return new MigrationVersion(matcher.group("version").replace("_", "."), matcher.group("name").replace("_", " "),
				repeatable);
	}

	/**
	 * Finds an optional target version in a {@link MigrationChain}.
	 * @param chain the chain in which to look for the target version
	 * @param value the value of the target version
	 * @return an optional version to stop migrating at
	 * @since 2.15.0
	 */
	static Optional<StopVersion> findTargetVersion(MigrationChain chain, String value) {

		if (value == null) {
			return Optional.empty();
		}

		try {
			var upperCaseValue = value.trim().toUpperCase();
			boolean optional;
			if (!upperCaseValue.endsWith("?")) {
				optional = false;
			}
			else {
				optional = true;
				upperCaseValue = upperCaseValue.substring(0, upperCaseValue.length() - 1);
			}
			var targetVersion = TargetVersion.valueOf(upperCaseValue);
			return chain.findTargetVersion(targetVersion).map(v -> new StopVersion(v, optional));
		}
		catch (IllegalArgumentException ex) {
			return Optional.of(parseStopVersion(value));
		}
	}

	/**
	 * Creates a {@link StopVersion} from the given class or file name in a more lenient
	 * way (the name for the migration is optional) and an optional flag.
	 * @param name a class or file name
	 * @return a {@link StopVersion}
	 * @throws MigrationsException if the name cannot be parsed
	 * @since 2.15.0
	 */
	private static StopVersion parseStopVersion(String name) {

		Matcher matcher = STOP_VERSION_PATTERN.matcher(name);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid class or file name for a migration: " + name);
		}

		boolean repeatable = "R".equalsIgnoreCase(matcher.group("type"));
		String optionalName = (matcher.group("name") == null) ? "n/a" : matcher.group("name").replace("_", " ");
		return new StopVersion(
				new MigrationVersion(matcher.group("version").replace("_", "."), optionalName, repeatable),
				matcher.group("op") != null);
	}

	/**
	 * Creates a {@link MigrationVersion} with a given value (the unique version
	 * identifier).
	 * @param value the unique version identifier
	 * @return a {@link MigrationVersion}
	 * @since 2.2.0
	 */
	public static MigrationVersion withValue(String value) {

		return withValue(value, false);
	}

	static MigrationVersion withValue(String value, boolean repeatable) {

		return withValueAndDescription(value, null, repeatable);
	}

	static MigrationVersion withValueAndDescription(String value, String description, boolean repeatable) {

		if (BASELINE_VALUE.equals(value)) {
			return MigrationVersion.baseline();
		}
		return new MigrationVersion(value, description, repeatable);
	}

	static MigrationVersion baseline() {

		return BASELINE;
	}

	static Comparator<MigrationVersion> getComparator(MigrationsConfig.VersionSortOrder versionSortOrder) {
		return switch (versionSortOrder) {
			case LEXICOGRAPHIC -> new VersionComparator();
			case SEMANTIC -> new SemanticVersionComparator();
		};
	}

	/**
	 * {@return the String value representing this version}
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Returns the {@literal true} if this version can be safely applied multiple times,
	 * even on checksum changes.
	 * @return {@literal true} if this version can be safely applied multiple times, even
	 * on checksum changes
	 * @since 1.13.3
	 */
	boolean isRepeatable() {
		return this.repeatable;
	}

	/**
	 * {@return the extracted description, maybe empty}
	 */
	Optional<String> getOptionalDescription() {
		return Optional.ofNullable(this.description);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MigrationVersion that = (MigrationVersion) o;
		return this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.value);
	}

	@Override
	public String toString() {
		return getValue();
	}

	/**
	 * Special values for some target versions. When migrating forwards, we will apply all
	 * migrations up to and including the target version. Migrations with a higher version
	 * number will be ignored. If the target is current, then no versioned migrations will
	 * be applied but repeatable migrations will be.
	 *
	 * @since 2.15.0
	 */
	public enum TargetVersion {

		/**
		 * Designates the current version of the schema.
		 */
		CURRENT,
		/**
		 * The latest version of the schema, as defined by the migration with the highest
		 * version.
		 */
		LATEST,
		/**
		 * The next version of the schema, as defined by the first pending migration.
		 */
		NEXT

	}

	private static final class SemanticVersionComparator implements Comparator<MigrationVersion> {

		@Override
		public int compare(MigrationVersion o1, MigrationVersion o2) {
			if (o1 == MigrationVersion.baseline() && o2 == MigrationVersion.baseline()) {
				return 0;
			}

			if (o1 == MigrationVersion.baseline() || o2 == MigrationVersion.baseline()) {
				return 1;
			}

			var lhs = Arrays.stream(o1.value.split("\\.")).map(Long::parseLong).toArray(Long[]::new);
			var rhs = Arrays.stream(o2.value.split("\\.")).map(Long::parseLong).toArray(Long[]::new);
			var max = Math.min(lhs.length, rhs.length);

			int i = 0;
			while (i < max) {
				int result = Long.compare(lhs[i], rhs[i]);
				if (result != 0) {
					return result;
				}
				++i;
			}

			if (lhs.length < rhs.length) {
				return (rhs[i] == 0) ? 0 : -1;
			}
			else if (lhs.length > rhs.length) {
				return (lhs[i] == 0) ? 0 : 1;
			}
			return 0;
		}

	}

	private static final class VersionComparator implements Comparator<MigrationVersion> {

		@Override
		public int compare(MigrationVersion o1, MigrationVersion o2) {
			if (o1 == MigrationVersion.baseline() && o2 == MigrationVersion.baseline()) {
				return 0;
			}

			if (o1 == MigrationVersion.baseline() || o2 == MigrationVersion.baseline()) {
				return 1;
			}

			return o1.getValue().compareTo(o2.getValue());
		}

	}

	/**
	 * Marks a version as stop version. If the version is marked as optional, migrations
	 * will go up to but not beyond the specified version. Otherwise, it will fail, if the
	 * version does not exist.
	 *
	 * @param version the version to stop at
	 * @param optional set to {@code true} if the version is not required to be in the
	 * chain.
	 */
	record StopVersion(MigrationVersion version, boolean optional) {
	}

}
