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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Formattable;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Michael J. Simons
 * @since TBA
 */
abstract class AbstractCatalogItem<T extends ItemType> implements CatalogItem<T>, Formattable {

	/**
	 * The unique name of this item.
	 */
	private final Name name;

	/**
	 * The type of this item.
	 */
	private final T type;

	/**
	 * The type of database entity targeted.
	 */
	private final TargetEntityType targetEntityType;

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

	AbstractCatalogItem(String name, T type, TargetEntityType targetEntityType, String identifier, Collection<String> properties, String options) {

		if (properties.isEmpty()) {
			throw new IllegalArgumentException("Constraints or indices require one or more properties.");
		}

		this.type = type;
		this.targetEntityType = targetEntityType;
		this.identifier = identifier;
		this.properties = new LinkedHashSet<>(properties);
		this.options = options;

		if (Strings.isBlank(name)) {
			this.name = Name.generate(this.getClass(), type, targetEntityType, identifier, properties, options);
		} else {
			this.name = Name.of(name);
		}
	}

	@Override
	public Name getName() {
		return name;
	}

	boolean hasName() {
		return !(name instanceof GeneratedName);
	}

	@Override
	public T getType() {
		return this.type;
	}

	/**
	 * @return The target entity of this item.
	 */
	public TargetEntityType getTargetEntityType() {
		return targetEntityType;
	}

	/**
	 * @return Identifier of this item to be used in create statements
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return Set of properties to be included with the item
	 */
	public Set<String> getProperties() {
		return properties;
	}

	/**
	 * @return Optional options to be passed down during creation of the item
	 */
	public Optional<String> getOptionalOptions() {
		return Strings.optionalOf(options);
	}

	@Override
	public boolean hasGeneratedName() {
		return this.getName() instanceof GeneratedName;
	}

	@Override
	public final void formatTo(Formatter formatter, int flags, int width, int precision) {

		Appendable out = formatter.out();
		try {
			out.append(this.getClass().getSimpleName().toUpperCase(Locale.ROOT));
			if (!(name instanceof GeneratedName)) {
				out.append(' ').append(name.getValue());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
		AbstractCatalogItem<?> that = (AbstractCatalogItem<?>) o;
		return getName().equals(that.getName()) && isEquivalentTo(that);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getType(), getTargetEntityType(), getIdentifier(), getProperties(), getOptionalOptions());
	}
}
