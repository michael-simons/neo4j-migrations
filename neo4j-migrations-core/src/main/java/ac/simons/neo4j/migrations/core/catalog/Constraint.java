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

import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A somewhat Neo4j version independent representation of a constraint.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
public final class Constraint extends AbstractCatalogItem<Constraint.Type> {

	/**
	 * Enumerates the different kinds of constraints.
	 */
	public enum Type implements ItemType {

		/**
		 * Unique constraints.
		 */
		UNIQUE,
		/**
		 * Existential constraints.
		 */
		EXISTS,
		/**
		 * Key constraints.
		 */
		KEY
	}

	/**
	 * Programmatic way of defining constraints.
	 */
	public interface Builder {

		/**
		 * Adds a name to the constraint
		 *
		 * @param name The new name
		 * @return The next step when building a constraint
		 */
		NamedBuilder named(String name);
	}

	/**
	 * Allows to specify the type of the constraint.
	 */
	public interface NamedBuilder {

		/**
		 * Creates a unique constraint for the given property
		 *
		 * @param property the property that should be unique
		 * @return the new constraint
		 */
		Constraint unique(String property);

		/**
		 * Creates an existential constraint for the given property
		 *
		 * @param property the property that is required to exist
		 * @return the new constraint
		 */
		Constraint exists(String property);

		/**
		 * Creates a key constraint for the given property
		 *
		 * @param properties the property that should be part of the key
		 * @return the new constraint
		 */
		Constraint key(String... properties);
	}

	private static class DefaultBuilder implements Builder, NamedBuilder {
		private final TargetEntity targetEntity;

		private final String identifier;

		private String name;

		private DefaultBuilder(TargetEntity targetEntity, String identifier) {
			this.targetEntity = targetEntity;
			this.identifier = identifier;
		}

		@Override
		public NamedBuilder named(String newName) {

			this.name = newName;
			return this;
		}

		@Override
		public Constraint unique(String property) {
			return new Constraint(name, Type.UNIQUE, targetEntity, identifier, Collections.singleton(property));
		}

		@Override
		public Constraint exists(String property) {
			return new Constraint(name, Type.EXISTS, targetEntity, identifier, Collections.singleton(property));
		}

		@Override
		public Constraint key(String... properties) {
			return new Constraint(name, Type.KEY, targetEntity, identifier, Arrays.asList(properties));
		}
	}

	/**
	 * Starts defining a new instance of a node constraint.
	 *
	 * @param label The label on which the constraint should be applied
	 * @return The ongoing builder
	 */
	public static Builder forNode(String label) {
		return new DefaultBuilder(TargetEntity.NODE, label);
	}

	/**
	 * Starts defining a new instance of a relationship constraint.
	 *
	 * @param type The type on which the constraint should be applied
	 * @return The ongoing builder
	 */
	public static Builder forRelationship(String type) {
		return new DefaultBuilder(TargetEntity.RELATIONSHIP, type);
	}

	/**
	 * Parses a constraint from a {@link MapAccessor}, which will either contain a result from {@code call db.constraints()}
	 * (Neo4j 3.5 upto 4.1) or from {@code SHOW CONSTRAINTS YIELD *} from Neo4j 4.2 and upwards
	 *
	 * @param row the result row
	 * @return A constraint
	 * @throws IllegalArgumentException if the row cannot be processed
	 */
	public static Constraint parse(MapAccessor row) {

		Value descriptionValue = row.get("description");
		Value nameValue = row.get("name");
		if (descriptionValue != Values.NULL) {
			String name = nameValue != Values.NULL ? nameValue.asString() : null;
			return parse(descriptionValue.asString(), name);
		}

		if (!REQUIRED_KEYS.stream().allMatch(row::containsKey)) {
			throw new IllegalArgumentException("Required keys are missing in the row describing the constraint");
		}

		String name = nameValue.asString();
		Constraint.Type type;
		switch (row.get(XMLSchemaConstants.TYPE).asString()) {
			case "NODE_KEY":
				type = Type.KEY;
				break;
			case "NODE_PROPERTY_EXISTENCE":
			case "RELATIONSHIP_PROPERTY_EXISTENCE":
				type = Type.EXISTS;
				break;
			case "UNIQUENESS":
				type = Type.UNIQUE;
				break;
			default:
				throw new IllegalArgumentException("Unsupported constraint type " + nameValue.asString());
		}

		TargetEntity targetEntity = TargetEntity.valueOf(row.get("entityType").asString());
		List<String> labelsOrTypes = row.get("labelsOrTypes").asList(Value::asString);
		List<String> properties = row.get(XMLSchemaConstants.PROPERTIES).asList(Value::asString);

		return new Constraint(name, type, targetEntity, labelsOrTypes.get(0), new LinkedHashSet<>(properties));
	}

	/**
	 * Creates a constraint from a xml definition.
	 *
	 * @param constraintElement as defined in {@code migration.xsd}.
	 * @return The new constraint if the element as parseable
	 */
	static Constraint parse(Element constraintElement) {

		String name = constraintElement.getAttribute(XMLSchemaConstants.NAME);
		Type type = Type.valueOf(constraintElement.getAttribute(XMLSchemaConstants.TYPE).toUpperCase(Locale.ROOT));
		NodeList labelOrType = constraintElement.getElementsByTagName(XMLSchemaConstants.LABEL);
		TargetEntity targetEntity;
		String identifier;
		if (labelOrType.getLength() == 0) {
			labelOrType = constraintElement.getElementsByTagName(XMLSchemaConstants.TYPE);
			targetEntity = TargetEntity.RELATIONSHIP;
		} else {
			targetEntity = TargetEntity.NODE;
		}
		identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) constraintElement
			.getElementsByTagName(XMLSchemaConstants.PROPERTIES).item(0)).getElementsByTagName(
			XMLSchemaConstants.PROPERTY);
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		NodeList optionsElement = constraintElement.getElementsByTagName(XMLSchemaConstants.OPTIONS);
		String options = null;
		if (optionsElement.getLength() == 1) {
			options = Arrays.stream(optionsElement.item(0).getTextContent()
				.split("\r?\n")).map(String::trim).collect(Collectors.joining("\n"));
		}

		return new Constraint(name, type, targetEntity, identifier, properties, options);
	}

	private static class PatternHolder {

		private final Pattern pattern;
		private final Constraint.Type type;
		private final TargetEntity targetEntity;

		PatternHolder(String pattern, Type type, TargetEntity targetEntity) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.type = type;
			this.targetEntity = targetEntity;
		}
	}

	private static final PatternHolder PATTERN_NODE_PROPERTY_IS_UNIQUE = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\(?\\k<var>\\.(?<properties>.+?)\\)? IS UNIQUE",
		Type.UNIQUE,
		TargetEntity.NODE
	);

	private static final PatternHolder PATTERN_NODE_PROPERTY_EXISTS = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT (?:exists)?\\((?<properties>\\k<var>\\..+?)\\)(?: IS NOT NULL)?",
		Type.EXISTS,
		TargetEntity.NODE
	);

	private static final PatternHolder PATTERN_NODE_KEY = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\((?<properties>\\k<var>\\..+?)\\) IS NODE KEY",
		Type.KEY,
		TargetEntity.NODE
	);

	private static final PatternHolder PATTERN_REL_PROPERTY_EXISTS = new PatternHolder(
		"CONSTRAINT ON \\(\\)-\\[\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?]-\\(\\) ASSERT (?:exists)?\\((?<properties>\\k<var>\\..+?)\\)(?: IS NOT NULL)?",
		Type.EXISTS,
		TargetEntity.RELATIONSHIP
	);

	private static final Set<String> REQUIRED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("name",
		XMLSchemaConstants.TYPE, "entityType", "labelsOrTypes", XMLSchemaConstants.PROPERTIES)));

	/**
	 * Creates a constraint from a [3.5, 4.1] database description
	 *
	 * @param description as returned by {@code CALL db.constraints}
	 * @param name        an optional name, might be {@literal null}
	 * @return The new constraint if the description is parseable
	 */
	private static Constraint parse(String description, String name) {

		Matcher matcher = null;
		PatternHolder match = null;
		for (PatternHolder patternHolder : new PatternHolder[] {
			PATTERN_NODE_PROPERTY_IS_UNIQUE,
			PATTERN_NODE_PROPERTY_EXISTS,
			PATTERN_NODE_KEY,
			PATTERN_REL_PROPERTY_EXISTS
		}
		) {
			matcher = patternHolder.pattern.matcher(description);
			if (matcher.matches()) {
				match = patternHolder;
				break;
			}
		}

		if (matcher.matches()) {
			String identifier = matcher.group("identifier").trim();
			String var = Pattern.quote(matcher.group("var") + ".");
			String propertiesGroup = matcher.group(XMLSchemaConstants.PROPERTIES);
			Stream<String> propertiesStream = match.type == Type.KEY ?
				Arrays.stream(propertiesGroup.split(", ")).map(String::trim) :
				Stream.of(propertiesGroup.trim());
			String[] properties = propertiesStream.map(s -> s.replaceFirst(var, "")).toArray(String[]::new);
			return new Constraint(name, match.type, match.targetEntity, identifier,
				new LinkedHashSet<>(Arrays.asList(properties)));
		}

		throw new IllegalArgumentException(
			String.format("The description '%s' does not match any known pattern.", description));
	}

	Constraint(Type type, TargetEntity targetEntity, String identifier, Collection<String> properties) {
		this(null, type, targetEntity, identifier, properties, null);
	}

	Constraint(String name, Type type, TargetEntity targetEntity, String identifier, Collection<String> properties) {
		this(name, type, targetEntity, identifier, properties, null);
	}

	Constraint(String name, Type type, TargetEntity targetEntity, String identifier, Collection<String> properties,
		String options) {
		super(name, type, targetEntity, identifier, properties, options);

		if (type == Type.KEY && getTarget() != TargetEntity.NODE) {
			throw new IllegalArgumentException("Key constraints are only supported for nodes, not for relationships.");
		}
	}

	Element toXML(Document document) {
		Element element = document.createElement(XMLSchemaConstants.CONSTRAINT);
		element.setAttribute(XMLSchemaConstants.NAME, getName().getValue());
		element.setIdAttribute(XMLSchemaConstants.NAME, true);
		element.setAttribute(XMLSchemaConstants.TYPE, getType().name().toLowerCase(Locale.ROOT));

		Element labelOrType;
		if (getTarget() == TargetEntity.NODE) {
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

	/**
	 * {@literal true}, if {@literal item} is a constraint of the same type for the same entity containing the same properties.
	 *
	 * @param item the other item to compare to
	 * @return {@literal true} if this and the other {@literal item} are aquivalent
	 */
	public boolean isEquivalentTo(CatalogItem<?> item) {

		if (this == item) {
			return true;
		}

		if (!(item instanceof Constraint)) {
			return false;
		}

		Constraint other = (Constraint) item;

		return this.getType().equals(other.getType()) &&
			this.getTarget().equals(other.getTarget()) &&
			this.getIdentifier().equals(other.getIdentifier()) &&
			this.getProperties().equals(other.getProperties());
	}
}
