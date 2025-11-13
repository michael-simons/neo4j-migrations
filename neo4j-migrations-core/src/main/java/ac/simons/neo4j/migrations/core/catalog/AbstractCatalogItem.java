/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.internal.Strings;
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for catalog items.
 *
 * @param <T> type of the concreate item
 * @author Michael J. Simons
 * @since 1.7.0
 */
abstract non-sealed class AbstractCatalogItem<T extends ItemType> implements CatalogItem<T> {

	/**
	 * Any additional options to be passed to the item. Might be {@literal null}.
	 */
	protected final String options;

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
	 * The set of properties to be included in the item. Not all Neo4j versions support
	 * more than one property for some or all given constraint or index types.
	 */
	private final Set<String> properties;

	AbstractCatalogItem(String name, T type, TargetEntityType targetEntityType, String identifier,
			Collection<String> properties, String options) {

		if (properties.isEmpty() && type != Index.Type.LOOKUP) {
			throw new IllegalArgumentException("Constraints or indices require one or more properties.");
		}

		this.type = type;
		this.targetEntityType = targetEntityType;
		this.identifier = identifier;
		this.properties = new LinkedHashSet<>(properties);
		this.options = options;

		if (Strings.isBlank(name)) {
			this.name = Name.generate(this.getClass(), type, targetEntityType, identifier, properties, options);
		}
		else {
			this.name = Name.of(name);
		}
	}

	/**
	 * Reads an optional {@code options} column from a row and formats it as a map that is
	 * renderable as Cypher.
	 * @param row row returned from {@code sHOW INDEXES} or {@code SHOW CONSTRAINTS}
	 * @return an optional options value
	 */
	static Optional<String> resolveOptions(MapAccessor row) {
		if (!row.containsKey("options")) {
			return Optional.empty();
		}

		Value options = row.get("options");
		if (options.isNull() || options.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(renderMap(options));
	}

	private static String renderMap(Value value) {
		if (TypeSystem.getDefault().MAP().isTypeOf(value)) {
			Map<String, Value> map = value.asMap(Function.identity());
			return map.entrySet()
				.stream()
				.map(e -> String.format("`%s`: %s", e.getKey(), renderMap(e.getValue())))
				.collect(Collectors.joining(", ", "{", "}"));
		}
		else if (TypeSystem.getDefault().LIST().isTypeOf(value)) {
			List<Value> list = value.asList(Function.identity());
			return list.stream().map(AbstractCatalogItem::renderMap).collect(Collectors.joining(", ", "[", "]"));
		}
		else {
			return value.toString();
		}
	}

	/**
	 * Extracts the target of the given item.
	 * @param element the element to extract the target from
	 * @return a target
	 */
	static Target extractTarget(Element element) {

		NodeList labelOrType = element.getElementsByTagName(XMLSchemaConstants.LABEL);
		TargetEntityType targetEntityType;
		if (labelOrType.getLength() == 0) {
			labelOrType = element.getElementsByTagName(XMLSchemaConstants.TYPE);
			targetEntityType = TargetEntityType.RELATIONSHIP;
		}
		else {
			targetEntityType = TargetEntityType.NODE;
		}
		return new Target(labelOrType.item(0).getTextContent(), targetEntityType);
	}

	/**
	 * Extracts the properties from the element into a set, guaranteeing order.
	 * @param element the element to extract properties from
	 * @return a sorted set
	 */
	static Set<String> extractProperties(Element element) {
		NodeList propertyNodes = ((Element) element.getElementsByTagName(XMLSchemaConstants.PROPERTIES).item(0))
			.getElementsByTagName(XMLSchemaConstants.PROPERTY);
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}
		return properties;
	}

	/**
	 * Extracts options child elements from the given element.
	 * @param constraintElement the element to extract options from
	 * @return optional options
	 */
	static String extractOptions(Element constraintElement) {
		NodeList optionsElement = constraintElement.getElementsByTagName(XMLSchemaConstants.OPTIONS);
		String options = null;
		if (optionsElement.getLength() == 1) {
			options = Arrays.stream(optionsElement.item(0).getTextContent().split("\r?\n"))
				.map(String::trim)
				.collect(Collectors.joining("\n"));
		}
		return options;
	}

	@Override
	public final Name getName() {
		return this.name;
	}

	boolean hasName() {
		return !(this.name instanceof GeneratedName);
	}

	@Override
	public T getType() {
		return this.type;
	}

	@Override
	public TargetEntityType getTargetEntityType() {
		return this.targetEntityType;
	}

	/**
	 * {@return the identifier of this item to be used in create statements}
	 */
	String getIdentifier() {
		return this.identifier;
	}

	@Override
	public Set<String> getProperties() {
		return this.properties;
	}

	/**
	 * {@return the optional options to be passed down during creation of the item}
	 */
	@Override
	public Optional<String> getOptionalOptions() {
		return Strings.optionalOf(this.options);
	}

	@Override
	public final boolean hasGeneratedName() {
		return this.getName() instanceof GeneratedName;
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
		return getName().equals(that.getName()) && getOptionalOptions().equals(that.getOptionalOptions())
				&& isEquivalentTo(that);
	}

	final Element toXML(Document document) {
		String elementName;
		String labelOrTypeContent;
		if (this instanceof Constraint) {
			elementName = XMLSchemaConstants.CONSTRAINT;
			labelOrTypeContent = getIdentifier();
		}
		else if (this instanceof Index index) {
			elementName = XMLSchemaConstants.INDEX;
			labelOrTypeContent = index.getDeconstructedIdentifiers()
				.stream()
				.map(s -> s.replace("|", "\\|"))
				.collect(Collectors.joining("|"));
		}
		else {
			throw new IllegalStateException("Unsupported subclass " + this.getClass());
		}
		Element element = document.createElement(elementName);
		element.setAttribute(XMLSchemaConstants.NAME, getName().getValue());
		element.setIdAttribute(XMLSchemaConstants.NAME, true);
		element.setAttribute(XMLSchemaConstants.TYPE, getType().getName());

		Element labelOrType;
		if (getTargetEntityType() == TargetEntityType.NODE) {
			labelOrType = document.createElement(XMLSchemaConstants.LABEL);
		}
		else {
			labelOrType = document.createElement(XMLSchemaConstants.TYPE);
		}
		labelOrType.setTextContent(labelOrTypeContent);
		element.appendChild(labelOrType);

		Element propertiesParentElement = document.createElement(XMLSchemaConstants.PROPERTIES);
		for (String propertyValue : getProperties()) {
			Element newElement = document.createElement(XMLSchemaConstants.PROPERTY);
			newElement.setTextContent(propertyValue);
			if (this instanceof Constraint constraint && constraint.getType() == Constraint.Type.PROPERTY_TYPE) {
				newElement.setAttribute("type", constraint.getPropertyType().getName());
			}
			propertiesParentElement.appendChild(newElement);
		}
		element.appendChild(propertiesParentElement);

		getOptionalOptions().ifPresent(optionsValue -> {
			Element newElement = document.createElement(XMLSchemaConstants.OPTIONS);
			newElement.setTextContent(optionsValue);
			element.appendChild(newElement);
		});

		return element;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getType(), getTargetEntityType(), getIdentifier(), getProperties(),
				getOptionalOptions());
	}

	record Target(String identifier, TargetEntityType targetEntityType) {
	}

}
