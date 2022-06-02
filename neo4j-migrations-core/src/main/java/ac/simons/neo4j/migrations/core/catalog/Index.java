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
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A somewhat Neo4j version independent representation of an index.
 *
 * @author Michael J. Simons
 * @since TBA
 */
public final class Index extends AbstractCatalogItem<Index.Type> {

	/**
	 * Enumerates the different kinds of indexes.
	 */
	public enum Type implements ItemType {
		PROPERTY,
		LOOKUP,
		FULLTEXT,
		CONSTRAINT_INDEX
	}

	static String[] labelsOrTypesKeys = {"tokenNames", "labelsOrTypes"};
	static String[] nameKeys = {"indexName", "name"};
	static String propertiesKey = "properties";
	static String entityTypeKey = "entityType";
	static String indexTypeKey = "type";
	static String uniquenessKey = "uniqueness";

	private static final Set<String> REQUIRED_KEYS_35 = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(nameKeys[0],
			indexTypeKey, labelsOrTypesKeys[0], propertiesKey)));

	private static final Set<String> REQUIRED_KEYS_40 = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(nameKeys[1],
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
	 * Allows to specify the type of the constraint.
	 */
	public interface NamedBuilder {

		/**
		 * Creates an index for the given properties
		 *
		 * @param properties the properties that should get indexed
		 * @return the new index
		 */
		Index properties(String... properties);

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
		public Index properties(String... properties) {
			return new Index(name, Type.PROPERTY, targetEntity, identifier, Arrays.asList(properties), "");
		}

	}

	/**
	 * Starts defining a new instance of a node property index.
	 *
	 * @param label The label on which the index should be applied
	 * @return The ongoing builder
	 */
	public static Builder forNode(String label) {
		return new DefaultBuilder(TargetEntityType.NODE, label);
	}

	/**
	 * Starts defining a new instance of a relationship property index.
	 *
	 * @param type The type on which the index should be applied
	 * @return The ongoing builder
	 */
	public static Builder forRelationship(String type) {
		return new DefaultBuilder(TargetEntityType.RELATIONSHIP, type);
	}

	Index(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties, String options) {
		super(name, type, targetEntityType, identifier, properties, options);
	}

	Index(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties) {
		super(name, type, targetEntityType, identifier, properties, "");
	}

	public Node toXML(Document document) {
		Element element = document.createElement(XMLSchemaConstants.INDEX);
		element.setAttribute(XMLSchemaConstants.NAME, getName().getValue());
		element.setIdAttribute(XMLSchemaConstants.NAME, true);
		element.setAttribute(XMLSchemaConstants.TYPE, getType().name().toLowerCase(Locale.ROOT));

		Element labelOrType;
		if (getTargetEntityType() == TargetEntityType.NODE) {
			labelOrType = document.createElement(XMLSchemaConstants.LABEL);
		} else {
			labelOrType = document.createElement(XMLSchemaConstants.TYPE);
		}
		labelOrType.setTextContent(getIdentifier());
		element.appendChild(labelOrType);

		Element properties = document.createElement(XMLSchemaConstants.PROPERTIES);
		for (String propertyValue : getProperties()) {
			Element property = document.createElement(XMLSchemaConstants.PROPERTY);
			property.setTextContent(propertyValue);
			properties.appendChild(property);
		}
		element.appendChild(properties);

		getOptionalOptions().ifPresent(optionsValue -> {
			Element options = document.createElement(XMLSchemaConstants.OPTIONS);
			options.setTextContent(optionsValue);
			element.appendChild(options);
		});

		return element;
	}


	public static Index parse(Element indexElement) {

		String name = indexElement.getAttribute(XMLSchemaConstants.NAME);
		String typeValue = indexElement.getAttribute(XMLSchemaConstants.TYPE);
		Index.Type type = Strings.isBlank(typeValue) ? Type.PROPERTY : Index.Type.valueOf(typeValue.toUpperCase(Locale.ROOT));
		NodeList labelOrType = indexElement.getElementsByTagName(XMLSchemaConstants.LABEL);
		TargetEntityType targetEntityType;
		if (labelOrType.getLength() == 0) {
			labelOrType = indexElement.getElementsByTagName(XMLSchemaConstants.TYPE);
			targetEntityType = TargetEntityType.RELATIONSHIP;
		} else {
			targetEntityType = TargetEntityType.NODE;
		}
		String identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) indexElement
				.getElementsByTagName(XMLSchemaConstants.PROPERTIES).item(0)).getElementsByTagName(
				XMLSchemaConstants.PROPERTY);
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		NodeList optionsElement = indexElement.getElementsByTagName(XMLSchemaConstants.OPTIONS);
		String options = null;
		if (optionsElement.getLength() == 1) {
			options = Arrays.stream(optionsElement.item(0).getTextContent()
					.split("\r?\n")).map(String::trim).collect(Collectors.joining("\n"));
		}

		return new Index(name, type, targetEntityType, identifier, new LinkedHashSet<>(properties), options);
	}

	/**
	 * Parses an index from a {@link MapAccessor}
	 *
	 * @param row the result row
	 * @return An index
	 * @throws IllegalArgumentException if the row cannot be processed
	 */
	public static Index parse(MapAccessor row) {


		if (!REQUIRED_KEYS_35.stream().allMatch(row::containsKey) && !REQUIRED_KEYS_40.stream().allMatch(row::containsKey)) {
			throw new IllegalArgumentException("Required keys are missing in the row describing the index");
		}

		List<String> labelsOrTypes = !row.get(labelsOrTypesKeys[0]).isNull()
				? row.get(labelsOrTypesKeys[0]).asList(Value::asString)
				: row.get(labelsOrTypesKeys[1]).asList(Value::asString);

		String name = !row.get(nameKeys[0]).isNull()
				? row.get(nameKeys[0]).asString()
				: row.get(nameKeys[1]).asString();

		String indexType = row.get(indexTypeKey).asString();
		String entityType = !row.get(entityTypeKey).isNull() ? row.get(entityTypeKey).asString() : "NODE";
		List<String> properties = row.get(propertiesKey).asList(Value::asString);
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

		return new Index(name, type, targetEntityType, String.join("|", labelsOrTypes), new LinkedHashSet<>(properties));
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
