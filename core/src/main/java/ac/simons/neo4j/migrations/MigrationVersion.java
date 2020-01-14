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
package ac.simons.neo4j.migrations;

import static ac.simons.neo4j.migrations.Defaults.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MigrationVersion {

	private static final String BASLINE_VALUE = "BASELINE";
	private static final MigrationVersion BASELINE = new MigrationVersion(BASLINE_VALUE, null);
	private static final Pattern VERSION_PATTERN = Pattern
		.compile("V(\\d+)__([\\w ]+)(?:\\." + CYPHER_SCRIPT_EXTENSION + ")?");

	private final String value;
	private final String description;

	static MigrationVersion of(Class<?> clazz) {
		return parse(clazz.getSimpleName());
	}

	static MigrationVersion parse(String simpleName) {

		Matcher matcher = VERSION_PATTERN.matcher(simpleName);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid class name for a migration: " + simpleName);
		}

		return new MigrationVersion(matcher.group(1), matcher.group(2).replaceAll("_", " "));
	}

	static MigrationVersion withValue(String value) {

		if (BASLINE_VALUE.equals(value)) {
			return MigrationVersion.baseline();
		}
		return new MigrationVersion(value, null);
	}

	static MigrationVersion baseline() {

		return BASELINE;
	}

	private MigrationVersion(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	/**
	 * @return The extracted description. Maybe null.
	 */
	String getDescription() {
		return description;
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
