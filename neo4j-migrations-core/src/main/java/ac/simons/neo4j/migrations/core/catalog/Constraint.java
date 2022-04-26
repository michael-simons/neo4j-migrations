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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
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
final class Constraint extends AbstractCatalogItem<Constraint.Type> {

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

	Constraint(String name, Type type, TargetEntity targetEntity, String identifier, Collection<String> properties) {
		this(name, type, targetEntity, identifier, properties, null);
	}

	Constraint(String name, Type type, TargetEntity targetEntity, String identifier, Collection<String> properties, String options) {
		super(name, type, targetEntity, identifier, properties, options);
	}
}
