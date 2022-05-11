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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.util.Formattable;
import java.util.Formatter;
import java.util.Objects;

/**
 * A value of a schema item.
 *
 * @author Michael J. Simons
 * @since TBA
 */
public final class Name implements Formattable, Id {

	/**
	 * Value of this name, might be {@literal null} or blank.
	 */
	private final String value;

	public static Name of(String value) {
		return new Name(value);
	}

	private Name(String value) {
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	boolean isBlank() {
		return Strings.isBlank(this.value);
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {

		if (!Strings.isBlank(value)) {
			formatter.format(" %s", value);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Name name = (Name) o;
		return Objects.equals(value, name.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
