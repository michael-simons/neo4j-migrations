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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import ac.simons.neo4j.migrations.annotations.proc.Label;

import java.util.Objects;

/**
 * @author Michael J. Simons
 * @soundtrack Ralf "Ralle" Petersen -  Album wird aus Hack gemacht 2016
 * @since TBA
 */
final class DefaultLabel implements Label {

	private final String value;

	/**
	 * Creates a label with the given value.
	 *
	 * @param value The value of this label, must not be null.
	 * @return A label with the given value.
	 */
	static Label of(String value) {

		return new DefaultLabel(value);
	}

	private DefaultLabel(String value) {
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override public String toString() {
		return "DefaultLabel{" +
			"value='" + value + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultLabel that = (DefaultLabel) o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
