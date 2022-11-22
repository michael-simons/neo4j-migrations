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

import ac.simons.neo4j.migrations.annotations.proc.SchemaName;

import java.util.Objects;

/**
 * @author Michael J. Simons
 * @soundtrack Ralf "Ralle" Petersen - Album wird aus Hack gemacht 2016
 * @since 1.11.0
 */
final class DefaultSchemaName implements SchemaName {

	enum Target {
		NODE, REL, UNDEFINED
	}

	private final String value;

	private final Target target;

	/**
	 * Creates a label with the given value.
	 *
	 * @param value The value of this label, must not be null.
	 * @return A label with the given value.
	 */
	static SchemaName label(String value) {

		return new DefaultSchemaName(value, Target.NODE);
	}

	/**
	 * Creates a type with the given value.
	 *
	 * @param value The value of this label, must not be null.
	 * @return A type with the given value.
	 */
	static SchemaName type(String value) {

		return new DefaultSchemaName(value, Target.REL);
	}

	private DefaultSchemaName(String value, Target target) {
		this.value = value;
		this.target = target;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "DefaultSchemaName{" +
			"value='" + value + '\'' +
			", target=" + target +
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
		DefaultSchemaName that = (DefaultSchemaName) o;
		return value.equals(that.value) && target == that.target;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, target);
	}
}
