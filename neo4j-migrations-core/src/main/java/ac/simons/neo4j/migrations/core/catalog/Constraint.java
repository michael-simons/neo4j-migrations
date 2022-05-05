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

import ac.simons.neo4j.migrations.core.MigrationsException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A somewhat Neo4j version independent representation of a constraint.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
final class Constraint extends AbstractCatalogItem<Constraint.Type> {


	private static class PatternAndType {

		private final Pattern pattern;
		private final Constraint.Type type;

		PatternAndType(String pattern, Type type) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.type = type;
		}
	}

	private static final PatternAndType PATTERN_NODE_PROPERTY_IS_UNIQUE = new PatternAndType(
		"CONSTRAINT ON \\(\\s?(?<name>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\(?\\k<name>\\.(?<properties>.+?)\\)? IS UNIQUE",
		Type.UNIQUE
	);

	private static final PatternAndType PATTERN_NODE_PROPERTY_EXISTS = new PatternAndType(
		"CONSTRAINT ON \\(\\s?(?<name>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT (?:exists)?\\(\\k<name>.(?<properties>.+?)\\)(?: IS NOT NULL)?",
		Type.EXISTS
	);

	private static final PatternAndType PATTERN_NODE_KEY = new PatternAndType(
		"CONSTRAINT ON \\(\\s?(?<name>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\((?<properties>.+?)\\) IS NODE KEY",
		Type.KEY
	);

	// TODO invoking
	// 3.5 call db.constraints()
	// 4.0 call db.constraints(), need to fill in name

	/**
	 * Creates a constraint from a (3.5) database description
	 *
	 * @param description as returned by {@code CALL db.constraints}, contains (optional) name and required description.
	 * @return The new constraint if the description is parseable
	 */
	public static Constraint of(ConstraintDescription description) {

		Matcher matcher = null;
		Type type = null;
		for (PatternAndType patternAndType : new PatternAndType[] {
			PATTERN_NODE_PROPERTY_IS_UNIQUE,
			PATTERN_NODE_PROPERTY_EXISTS,
			PATTERN_NODE_KEY }
		) {
			matcher = patternAndType.pattern.matcher(description.getValue());
			if (matcher.matches()) {
				type = patternAndType.type;
				break;
			}
		}

		if (matcher.matches()) {
			String identifier = matcher.group("identifier").trim();
			String name = Pattern.quote(matcher.group("name") + ".");
			String propertiesGroup = matcher.group("properties");
			Stream<String> propertiesStream = type == Type.KEY ? Arrays.stream(propertiesGroup.split(", ")).map(String::trim) : Stream.of(propertiesGroup.trim());
			String[] properties = propertiesStream.map(s -> s.replaceFirst(name, "")).toArray(String[]::new);
			return new Constraint(description.getName(), type, TargetEntity.NODE, identifier,
				new LinkedHashSet<>(Arrays.asList(properties)));
		}

		throw new MigrationsException(String.format("Could not parse %s", description.getValue()));
	}

	/**
	 * Creates a constraint from a xml definition.
	 *
	 * @param constraintElement as defined in {@code migration.xsd}.
	 * @return The new constraint if the element as parseable
	 */
	public static Constraint of(Element constraintElement) {

		String name = constraintElement.getAttribute("id");
		Type type = Type.valueOf(constraintElement.getAttribute("type").toUpperCase(Locale.ROOT));
		NodeList labelOrType = constraintElement.getElementsByTagName("label");
		TargetEntity targetEntity;
		String identifier;
		if (labelOrType.getLength() == 0) {
			labelOrType = constraintElement.getElementsByTagName("type");
			targetEntity = TargetEntity.RELATIONSHIP;
		} else {
			targetEntity = TargetEntity.NODE;
		}
		identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) constraintElement
			.getElementsByTagName("properties").item(0)).getElementsByTagName("property");
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		NodeList optionsElement = constraintElement.getElementsByTagName("options");
		String options = null;
		if (optionsElement.getLength() == 1) {
			options = Arrays.stream(optionsElement.item(0).getTextContent()
				.split("\r?\n")).map(String::trim).collect(Collectors.joining("\n"));
		}

		return new Constraint(name, type, targetEntity, identifier, properties, options);
	}

	/**
	 * Enumerates the different kinds of constraints.
	 */
	enum Type implements CatalogItemType {
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
