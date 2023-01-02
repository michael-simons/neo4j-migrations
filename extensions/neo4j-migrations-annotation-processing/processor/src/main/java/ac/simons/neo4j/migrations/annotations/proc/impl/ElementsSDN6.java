/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Value holder of SDN6 types.
 *
 * @author Michael J. Simons
 * @soundtrack Motörhead - Iron Fist
 * @since 1.15.0
 */
final class ElementsSDN6 {

	static Optional<ElementsSDN6> of(Elements elements) {
		TypeElement sdn6Node = elements.getTypeElement(FullyQualifiedNames.SDN6_NODE);
		if (sdn6Node == null) {
			return Optional.empty();
		}
		TypeElement sdn6Relationship = elements.getTypeElement(FullyQualifiedNames.SDN6_RELATIONSHIP);
		ExecutableElement sdn6NodeValue = Attributes.get(sdn6Node, Attributes.VALUE).orElseThrow(NoSuchElementException::new);
		ExecutableElement sdn6NodeLabels = Attributes.get(sdn6Node, Attributes.LABELS).orElseThrow(NoSuchElementException::new);
		ExecutableElement sdn6NodePrimaryLabel = Attributes.get(sdn6Node, Attributes.PRIMARY_LABEL).orElseThrow(NoSuchElementException::new);

		TypeElement sdn6Property = elements.getTypeElement(FullyQualifiedNames.SDN6_PROPERTY);

		TypeElement sdn6Id = elements.getTypeElement(FullyQualifiedNames.SDN6_ID);
		TypeElement sdn6GeneratedValue = elements.getTypeElement(FullyQualifiedNames.SDN6_GENERATED_VALUE);
		TypeElement commonsId = elements.getTypeElement(FullyQualifiedNames.COMMONS_ID);

		return Optional.of(new ElementsSDN6(sdn6Node, sdn6NodeValue, sdn6NodeLabels, sdn6Relationship, sdn6NodePrimaryLabel, sdn6Id, sdn6GeneratedValue, commonsId, sdn6Property));
	}

	private final TypeElement node;
	private final ExecutableElement nodeValue;
	private final ExecutableElement nodeLabels;
	private final TypeElement relationship;
	private final ExecutableElement nodePrimaryLabel;
	private final TypeElement id;
	private final TypeElement generatedValue;
	private final TypeElement commonsId;

	private final TypeElement property;

	@SuppressWarnings("squid:S107") // That's what you get for back-porting a record to JDK 8
	private ElementsSDN6(
		TypeElement node, ExecutableElement nodeValue, ExecutableElement nodeLabels, TypeElement relationship,
		ExecutableElement nodePrimaryLabel, TypeElement id, TypeElement generatedValue,
		TypeElement commonsId, TypeElement property) {
		this.node = node;
		this.nodeValue = nodeValue;
		this.nodeLabels = nodeLabels;
		this.relationship = relationship;
		this.nodePrimaryLabel = nodePrimaryLabel;
		this.id = id;
		this.generatedValue = generatedValue;
		this.commonsId = commonsId;
		this.property = property;
	}

	public TypeElement node() {
		return node;
	}

	public ExecutableElement nodeValue() {
		return nodeValue;
	}

	public ExecutableElement nodeLabels() {
		return nodeLabels;
	}

	public TypeElement relationship() {
		return relationship;
	}

	public ExecutableElement nodePrimaryLabel() {
		return nodePrimaryLabel;
	}

	public TypeElement id() {
		return id;
	}

	public TypeElement generatedValue() {
		return generatedValue;
	}

	public TypeElement commonsId() {
		return commonsId;
	}

	public TypeElement property() {
		return property;
	}
}
