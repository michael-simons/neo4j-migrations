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
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.w3c.dom.Element;

/**
 * A somewhat Neo4j version independent representation of an index.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since TBA
 */
public final class Index extends AbstractCatalogItem<Index.Type> {

	/**
	 * Enumerates the different kinds of indexes.
	 */
	public enum Type implements ItemType {
		PROPERTY,
		/**
		 * A lookup index. Those indexes are paramount for the database to perform proper. They
		 * provide mapping from labels to nodes, or from relationships types to relationships, instead of between
		 * properties and entities and should usually not be changed.
		 */
		LOOKUP,
		/**
		 * Fulltext indexes for long text properties.
		 */
		FULLTEXT,
		CONSTRAINT_INDEX;

		@Override
		public String getName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static String[] labelsOrTypesKeys = { "tokenNames", "labelsOrTypes" };
	static String[] nameKeys = { "indexName", "name" };
	static String propertiesKey = "properties";
	static String entityTypeKey = "entityType";
	static String indexTypeKey = "type";
	static String uniquenessKey = "uniqueness";

	private static final Set<String> REQUIRED_KEYS_35 = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(nameKeys[0],
			indexTypeKey, labelsOrTypesKeys[0], propertiesKey)));

	private static final Set<String> REQUIRED_KEYS_40 = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(nameKeys[1],
			indexTypeKey, entityTypeKey, labelsOrTypesKeys[1], propertiesKey)));

	/**
	 * Programmatic way of defining indexes.
	 */
	public interface Builder {

		/**
		 * Adds a name to the index
		 *
		 * @param name The new name
		 * @return The next step when building an index
		 */
		NamedBuilder named(String name);
	}

	/**
	 * Allows to specify the type of the constraint. Not all types are exposed, they should usually not be touch manually.
	 */
	public interface NamedBuilder {

		/**
		 * Creates a fulltext index
		 * @param properties The properties to be included in the index
		 * @return the next step
		 */
		Index fulltext(String... properties);

		/**
		 * Creates a property index
		 * @param properties The properties to be included in the index
		 * @return the next step
		 */
		Index onProperties(String... properties);
	}

	private static class DefaultBuilder implements Builder, NamedBuilder {
		private final TargetEntityType targetEntity;

		private final String identifier;

		private String name;

		private DefaultBuilder(TargetEntityType targetEntity, String identifier) {
			this.targetEntity = targetEntity;
			this.identifier = identifier;
		}

		@Override
		public NamedBuilder named(String newName) {

			this.name = newName;
			return this;
		}

		@Override
		public Index onProperties(String... properties) {
			return new Index(name, Type.PROPERTY, targetEntity, identifier, Arrays.asList(properties), "");
		}

		@Override
		public Index fulltext(String... properties) {
			return new Index(name, Type.FULLTEXT, targetEntity, identifier, Arrays.asList(properties), "");
		}
	}

	/**
	 * Starts defining a new instance of a node property index.
	 *
	 * @param labels The labels on which the index should be applied
	 * @return The ongoing builder
	 */
	public static Builder forNode(String... labels) {
		return new DefaultBuilder(TargetEntityType.NODE, String.join("|", labels));
	}

	/**
	 * Starts defining a new instance of a relationship property index.
	 *
	 * @param types The type on which the index should be applied
	 * @return The ongoing builder
	 */
	public static Builder forRelationship(String... types) {
		return new DefaultBuilder(TargetEntityType.RELATIONSHIP, String.join("|", types));
	}

	Index(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties,
		String options) {
		super(name, type, targetEntityType, identifier, properties, options);
	}

	Index(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties) {
		super(name, type, targetEntityType, identifier, properties, "");
	}

	/**
	 * Creates an index from a xml definition.
	 *
	 * @param indexElement as defined in {@code migration.xsd}.
	 * @return The new index if the element as parseable
	 */
	public static Index parse(Element indexElement) {

		String name = indexElement.getAttribute(XMLSchemaConstants.NAME);
		String typeValue = indexElement.getAttribute(XMLSchemaConstants.TYPE);
		Index.Type type = Strings.isBlank(typeValue) ?
			Type.PROPERTY :
			Index.Type.valueOf(typeValue.toUpperCase(Locale.ROOT));
		Target target = extractTarget(indexElement);

		Set<String> properties = extractProperties(indexElement);
		String options = extractOptions(indexElement);

		return new Index(name, type, target.targetEntityType(), target.identifier(), new LinkedHashSet<>(properties),
			options);
	}

	/**
	 * Parses an index from a {@link MapAccessor}
	 *
	 * @param row the result row
	 * @return An index
	 * @throws IllegalArgumentException if the row cannot be processed
	 */
	public static Index parse(MapAccessor row) {

		if (!REQUIRED_KEYS_35.stream().allMatch(row::containsKey) && !REQUIRED_KEYS_40.stream()
			.allMatch(row::containsKey)) {
			throw new IllegalArgumentException("Required keys are missing in the row describing the index");
		}

		List<String> labelsOrTypes = !row.get(labelsOrTypesKeys[0]).isNull()
			? row.get(labelsOrTypesKeys[0]).asList(Value::asString)
			: row.get(labelsOrTypesKeys[1]).isNull() // lookup index
			? Collections.emptyList()
			: row.get(labelsOrTypesKeys[1]).asList(Value::asString);

		String name = !row.get(nameKeys[0]).isNull()
			? row.get(nameKeys[0]).asString()
			: row.get(nameKeys[1]).asString();

		String indexType = row.get(indexTypeKey).asString();
		String entityType = !row.get(entityTypeKey).isNull() ? row.get(entityTypeKey).asString() : "NODE";
		List<String> properties = row.get(propertiesKey).isNull() // lookup index
			? Collections.emptyList()
			: row.get(propertiesKey).asList(Value::asString);
		TargetEntityType targetEntityType = TargetEntityType.valueOf(entityType);

		Index.Type type;
		switch (indexType) {
			case "LOOKUP":
				type = Type.LOOKUP;
				break;
			case "node_label_property":
			case "BTREE":
				type = Type.PROPERTY;
				break;
			case "FULLTEXT":
			case "node_fulltext":
				type = Type.FULLTEXT;
				break;
			case "node_unique_property":
				type = Type.CONSTRAINT_INDEX;
				break;
			default:
				throw new IllegalArgumentException("Unsupported index type " + name);
		}

		// Neo4j 4.x defines the index via uniqueness property
		type = row.get(uniquenessKey).isNull() || !row.get(uniquenessKey).asString().equals("UNIQUE")
			? type
			: Type.CONSTRAINT_INDEX;

		return new Index(name, type, targetEntityType, String.join("|", labelsOrTypes),
			new LinkedHashSet<>(properties));
	}

	/**
	 * {@literal true}, if {@literal item} is an index of the same type for the same entity containing the same properties.
	 * Also index name and options will be compared.
	 *
	 * @param that the other item to compare to
	 * @return {@literal true} if this and the other {@literal item} are equivalent
	 */
	@Override
	public boolean isEquivalentTo(CatalogItem<?> that) {

		if (this == that) {
			return true;
		}

		if (!(that instanceof Index)) {
			return false;
		}

		Index other = (Index) that;

		return this.getType().equals(other.getType()) &&
				this.getTargetEntityType().equals(other.getTargetEntityType()) &&
				this.getIdentifier().equals(other.getIdentifier()) &&
				this.getProperties().equals(other.getProperties()) &&
				this.getOptionalOptions().equals(other.getOptionalOptions());
	}
}
