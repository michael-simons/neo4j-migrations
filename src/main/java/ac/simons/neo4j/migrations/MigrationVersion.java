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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A migrations version.
 *
 * @author Michael J. Simons
 */
public final class MigrationVersion implements Comparable<MigrationVersion> {

	private static final MigrationVersion BASELINE = withValue("BASELINE");
	private final String value;

	static MigrationVersion of(Class<?> clazz) {
		return parse(clazz.getSimpleName());
	}

	static MigrationVersion parse(String simpleName) {

		Matcher matcher = Pattern.compile("V(\\d+)__\\w+").matcher(simpleName);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid class name for a migration: " + simpleName);
		}

		return new MigrationVersion(matcher.group(1));
	}

	static MigrationVersion withValue(String value) {

		return new MigrationVersion(value);
	}

	static MigrationVersion baseline() {

		return BASELINE;
	}

	public MigrationVersion(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public int compareTo(MigrationVersion o) {

		if (BASELINE.getValue().equals(this.value)) {
			return -1;
		}
		return value.compareTo(o.getValue());
	}
}
