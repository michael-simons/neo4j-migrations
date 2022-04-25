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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A somewhat Neo4j version independent representation of a constraint.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
final class Constraint implements CatalogItem {

	public static Constraint of(Element constraintElement) {

		String name = constraintElement.getAttribute("id");
		Kind kind = Kind.valueOf(constraintElement.getAttribute("kind").toUpperCase(Locale.ROOT));
		NodeList labelOrType = constraintElement.getElementsByTagName("label");
		Target target;
		String identifier;
		if (labelOrType.getLength() == 0) {
			labelOrType = constraintElement.getElementsByTagName("type");
			target = Target.RELATIONSHIP;
		} else {
			target = Target.NODE;
		}
		identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) constraintElement
			.getElementsByTagName("properties").item(0)).getElementsByTagName("property");
		Set<String> properties = new LinkedHashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		NodeList optionanElement = constraintElement.getElementsByTagName("options");
		String options = null;
		if (optionanElement.getLength() == 1) {
			options = Arrays.stream(optionanElement.item(0).getTextContent()
				.split("\r?\n")).map(String::trim).collect(Collectors.joining("\n"));
		}

		return new Constraint(name, kind, target, identifier, properties, options);
	}

	/**
	 * Enumerates the different kinds of constraints.
	 */
	enum Kind {
		UNIQUE,
		EXISTS,
		KEY
	}

	/**
	 * Enumerates possible target entities of constraints.
	 */
	enum Target {
		NODE,
		RELATIONSHIP
	}

	/**
	 * The name of this constraint, equivalent to the id of the element in the xml scheme.
	 */
	private final String name;

	/**
	 * The type of this constraint.
	 */
	private final Kind kind;

	/**
	 * The type of database entity targeted.
	 */
	private final Target target;

	/**
	 * The identifier of that entity, either a label or a (relationship) type.
	 */
	private final String identifier;

	/**
	 * The set of properties to be included in the constraint. Not all Neo4j versions support more than one property
	 * for some given constraint types.
	 */
	private final Set<String> properties;

	/**
	 * Any additional options to be passed to the constraint. Might be {@literal null}.
	 */
	private final String options;

	Constraint(String name, Kind kind, Target target, String identifier, Collection<String> properties) {
		this(name, kind, target, identifier, properties, null);
	}

	Constraint(String name, Kind kind, Target target, String identifier, Collection<String> properties, String options) {

		if (properties.isEmpty()) {
			throw new IllegalArgumentException("Constraints require one or more properties.");
		}

		this.name = name;
		this.kind = kind;
		this.target = target;
		this.identifier = identifier;
		this.properties = new LinkedHashSet<>(properties);
		this.options = options;
	}

	@Override
	public String getId() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
	}

	public Target getTarget() {
		return target;
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
}
