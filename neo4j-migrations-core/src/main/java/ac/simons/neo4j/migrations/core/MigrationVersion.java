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

	private static final String BASELINE_VALUE = "BASELINE";
	private static final MigrationVersion BASELINE = new MigrationVersion(BASELINE_VALUE, null, false);
	@SuppressWarnings("squid:S5843") // This is a fine regex
	static final Pattern VERSION_PATTERN = Pattern.compile("(?<type>[VR])(?<version>\\d+(?:_\\d+)*+|\\d+(?:\\.\\d+)*+)__(?<name>[\\w ]+)(?:\\.(?<ext>\\w+))?");

	private final String value;
	private final String description;
	/**
	 * A flag indicating that this version can be safely repeated, even on checksum changes.
	 * @since 1.13.3
	 */
	private final boolean repeatable;

	/**
	 * @param pathOrUrl A string representing either a path or an URL.
	 * @return {@literal true} when the given path or URL can be parsed into a valid {@link MigrationVersion}
	 */
	public static boolean canParse(String pathOrUrl) {
		return VERSION_PATTERN.matcher(pathOrUrl).find();
	}

	static MigrationVersion of(Class<?> clazz) {
		return parse(clazz.getSimpleName());
	}

	static MigrationVersion parse(String simpleName) {

		Matcher matcher = VERSION_PATTERN.matcher(simpleName);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid class name for a migration: " + simpleName);
		}

		boolean repeatable = "R".equalsIgnoreCase(matcher.group("type"));
		return new MigrationVersion(matcher.group("version").replace("_", "."), matcher.group("name").replace("_", " "), repeatable);
	}

	static MigrationVersion withValue(String value) {

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

	private MigrationVersion(String value, String description, boolean repeatable) {
		this.value = value;
		this.description = description;
		this.repeatable = repeatable;
	}

	/**
	 * @return the {@link String} value representing this version
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return {@literal true} if this version can be safely applied multiple times, even on checksum changes
	 * @since 1.13.3
	 * @deprecated since 1.14.0, it should not have been public in the first place, no replacement
	 */
	@SuppressWarnings({"DeprecatedIsStillUsed", "squid:S1133"})
	@Deprecated
	public boolean isRepeatable() {
		return repeatable;
	}

	/**
	 * @return The extracted description. Maybe null.
	 */
	Optional<String> getOptionalDescription() {
		return Optional.ofNullable(description);
	}

	@Override
	public String toString() {
		return getValue();
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
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	static class VersionComparator implements Comparator<MigrationVersion> {

		@Override
		public int compare(MigrationVersion o1, MigrationVersion o2) {
			if (o1 == MigrationVersion.baseline()) {
				return -1;
			}

			return o1.getValue().compareTo(o2.getValue());
		}
	}
}
