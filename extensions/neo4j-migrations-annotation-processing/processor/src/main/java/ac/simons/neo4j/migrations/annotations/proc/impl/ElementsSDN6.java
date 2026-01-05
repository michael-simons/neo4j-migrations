/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Value holder of SDN6 types.
 *
 * @param node corresponding field in the class
 * @param nodeValue corresponding field in the class
 * @param nodeLabels corresponding field in the class
 * @param relationshipProperties corresponding field in the class
 * @param nodePrimaryLabel corresponding field in the class
 * @param id corresponding field in the class
 * @param generatedValue corresponding field in the class
 * @param commonsId corresponding field in the class
 * @param property corresponding field in the class
 * @param transientProperty corresponding field in the class
 * @author Michael J. Simons
 * @since 1.15.0
 */
record ElementsSDN6(TypeElement node, ExecutableElement nodeValue, ExecutableElement nodeLabels,
		TypeElement relationshipProperties, ExecutableElement nodePrimaryLabel, TypeElement id,
		TypeElement generatedValue, TypeElement commonsId, TypeElement property, TypeElement transientProperty) {

	static Optional<ElementsSDN6> of(Elements elements) {
		TypeElement sdn6Node = elements.getTypeElement(FullyQualifiedNames.SDN6_NODE);
		if (sdn6Node == null) {
			return Optional.empty();
		}
		TypeElement sdn6Relationship = elements.getTypeElement(FullyQualifiedNames.SDN6_RELATIONSHIP_PROPERTIES);
		ExecutableElement sdn6NodeValue = Attributes.get(sdn6Node, Attributes.VALUE).orElseThrow();
		ExecutableElement sdn6NodeLabels = Attributes.get(sdn6Node, Attributes.LABELS).orElseThrow();
		ExecutableElement sdn6NodePrimaryLabel = Attributes.get(sdn6Node, Attributes.PRIMARY_LABEL).orElseThrow();

		TypeElement sdn6Property = elements.getTypeElement(FullyQualifiedNames.SDN6_PROPERTY);

		TypeElement sdn6Id = elements.getTypeElement(FullyQualifiedNames.SDN6_ID);
		TypeElement sdn6GeneratedValue = elements.getTypeElement(FullyQualifiedNames.SDN6_GENERATED_VALUE);
		TypeElement commonsId = elements.getTypeElement(FullyQualifiedNames.COMMONS_ID);
		TypeElement transientProperty = elements.getTypeElement(FullyQualifiedNames.COMMONS_TRANSIENT);

		return Optional.of(new ElementsSDN6(sdn6Node, sdn6NodeValue, sdn6NodeLabels, sdn6Relationship,
				sdn6NodePrimaryLabel, sdn6Id, sdn6GeneratedValue, commonsId, sdn6Property, transientProperty));
	}
}
