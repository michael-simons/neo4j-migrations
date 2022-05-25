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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Michael J. Simons
 * @since TBA
 */
abstract class AbstractCatalogItem<T extends ItemType> implements CatalogItem<T> {

	/**
	 * The name of this item, equivalent to the id of the element in the xml scheme.
	 */
	private final Name name;

	/**
	 * The type of this item.
	 */
	private final T type;

	/**
	 * The type of database entity targeted.
	 */
	private final TargetEntity targetEntity;

	/**
	 * The identifier of that entity, either a label or a (relationship) type.
	 */
	private final String identifier;

	/**
	 * The set of properties to be included in the item. Not all Neo4j versions support more than one property
	 * for some or all given constraint or index types.
	 */
	private final Set<String> properties;

	/**
	 * Any additional options to be passed to the item. Might be {@literal null}.
	 */
	private final String options;

	AbstractCatalogItem(String name, T type, TargetEntity targetEntity, String identifier, Collection<String> properties,
		String options) {

		if (properties.isEmpty()) {
			throw new IllegalArgumentException("Constraints or indices require one or more properties.");
		}

		this.name = Name.of(name);
		this.type = type;
		this.targetEntity = targetEntity;
		this.identifier = identifier;
		this.properties = new LinkedHashSet<>(properties);
		this.options = options;
	}

	@Override
	public Id getId() {
		if (name.isBlank()) {
			return GeneratedId.of(this);
		}
		return name;
	}

	public Name getName() {
		return name;
	}

	public boolean hasName() {
		return !name.isBlank();
	}

	@Override
	public T getType() {
		return this.type;
	}

	public TargetEntity getTarget() {
		return targetEntity;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Set<String> getProperties() {
		return properties;
	}

	public Optional<String> getOptionalOptions() {
		return Strings.optionalOf(options);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
			"name=" + name +
			", type=" + type +
			", targetEntity=" + targetEntity +
			", identifier='" + identifier + '\'' +
			", properties=" + String.join(",", properties) +
			", options='" + options + '\'' +
			'}';
	}
}
