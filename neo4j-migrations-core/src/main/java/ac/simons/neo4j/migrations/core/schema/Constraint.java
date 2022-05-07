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
package ac.simons.neo4j.migrations.core.schema;

import ac.simons.neo4j.migrations.core.MigrationsException;

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
public final class Constraint extends AbstractSchemaItem<Constraint.Type> {

	// Constants used throughout parsing and reading / writing elements and attributes
	private static final String KW_LABEL = "label";
	private static final String KW_PROPERTY = "property";
	private static final String KW_OPTIONS = "options";

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

	public static final String KW_TYPE = "type";
	public static final String KW_PROPERTIES = "properties";
	private static final Set<String> REQUIRED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("name",
		KW_TYPE, "entityType", "labelsOrTypes", KW_PROPERTIES)));

	// TODO invoking
	// 3.5 call db.constraints()
	// 4.0 call db.constraints(), need to fill in name
	// 4.1 call db.constraints(), need to fill in name
	// 4.2 SHOW CONSTRAINTS

	static Constraint parse(MapAccessor row, ParseContext context) {

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
		switch (row.get(KW_TYPE).asString()) {
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
		List<String> properties = row.get(KW_PROPERTIES).asList(Value::asString);

		return new Constraint(name, type, targetEntity, labelsOrTypes.get(0), new LinkedHashSet<>(properties));
	}

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
			String propertiesGroup = matcher.group(KW_PROPERTIES);
			Stream<String> propertiesStream = match.type == Type.KEY ?
				Arrays.stream(propertiesGroup.split(", ")).map(String::trim) :
				Stream.of(propertiesGroup.trim());
			String[] properties = propertiesStream.map(s -> s.replaceFirst(var, "")).toArray(String[]::new);
			return new Constraint(name, match.type, match.targetEntity, identifier,
				new LinkedHashSet<>(Arrays.asList(properties)));
		}

		throw new MigrationsException(String.format("Could not parse %s", description));
	}

	/**
	 * Creates a constraint from a xml definition.
	 *
	 * @param constraintElement as defined in {@code migration.xsd}.
	 * @return The new constraint if the element as parseable
	 */
	static Constraint parse(Element constraintElement) {

		String name = constraintElement.getAttribute("id");
		Type type = Type.valueOf(constraintElement.getAttribute(KW_TYPE).toUpperCase(Locale.ROOT));
		NodeList labelOrType = constraintElement.getElementsByTagName(KW_LABEL);
		TargetEntity targetEntity;
		String identifier;
		if (labelOrType.getLength() == 0) {
			labelOrType = constraintElement.getElementsByTagName(KW_TYPE);
			targetEntity = TargetEntity.RELATIONSHIP;
		} else {
			targetEntity = TargetEntity.NODE;
		}
		identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) constraintElement
			.getElementsByTagName(KW_PROPERTIES).item(0)).getElementsByTagName(KW_PROPERTY);
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		NodeList optionsElement = constraintElement.getElementsByTagName(KW_OPTIONS);
		String options = null;
		if (optionsElement.getLength() == 1) {
			options = Arrays.stream(optionsElement.item(0).getTextContent()
				.split("\r?\n")).map(String::trim).collect(Collectors.joining("\n"));
		}

		return new Constraint(name, type, targetEntity, identifier, properties, options);
	}

	Element toXML(Document document) {
		Element element = document.createElement("constraint");
		element.setAttribute("id", getId().getValue());
		element.setIdAttribute("id", true);
		element.setAttribute(KW_TYPE, getType().name().toLowerCase(Locale.ROOT));

		Element labelOrType;
		if (getTarget() == TargetEntity.NODE) {
			labelOrType = document.createElement(KW_LABEL);
		} else {
			labelOrType = document.createElement(KW_TYPE);
		}
		labelOrType.setTextContent(getIdentifier());
		element.appendChild(labelOrType);

		Element properties = document.createElement(KW_PROPERTIES);
		for (String propertyValue : getProperties()) {
			Element property = document.createElement(KW_PROPERTY);
			property.setTextContent(propertyValue);
			properties.appendChild(property);
		}
		element.appendChild(properties);

		getOptionalOptions().ifPresent(optionsValue -> {
			Element options = document.createElement(KW_OPTIONS);
			options.setTextContent(optionsValue);
			element.appendChild(options);
		});

		return element;
	}

	/**
	 * Enumerates the different kinds of constraints.
	 */
	public enum Type implements ItemType {
		UNIQUE,
		EXISTS,
		KEY
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
	}
}
