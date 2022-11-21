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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Value holder of OGM types.
 *
 * @author Michael J. Simons
 * @soundtrack Motörhead - Iron Fist
 * @since 1.15.0
 */
record ElementsOGM(TypeElement node, ExecutableElement nodeValue, ExecutableElement nodeLabel,
	TypeElement relationship, ExecutableElement relationshipType,
	ExecutableElement relationshipValue, TypeElement property, TypeElement id,
	TypeElement generatedValue, TypeElement compositeIndexes,
	ExecutableElement compositeIndexesValue, TypeElement compositeIndex,
	ExecutableElement compositeIndexValue, ExecutableElement compositeIndexProperties,
	ExecutableElement compositeIndexUnique, TypeElement index, ExecutableElement indexUnique,
	TypeElement required) {

	static Optional<ElementsOGM> of(Elements elements) {
		TypeElement ogmNode = elements.getTypeElement(FullyQualifiedNames.OGM_NODE);
		if (ogmNode == null) {
			return Optional.empty();
		}

		ExecutableElement ogmNodeValue = Attributes.get(ogmNode, Attributes.VALUE).orElseThrow();
		ExecutableElement ogmNodeLabel = Attributes.get(ogmNode, Attributes.LABEL).orElseThrow();

		TypeElement ogmRelationship = elements.getTypeElement(FullyQualifiedNames.OGM_RELATIONSHIP);
		ExecutableElement ogmRelationshipValue = Attributes.get(ogmRelationship, Attributes.VALUE).orElseThrow();
		ExecutableElement ogmRelationshipType = Attributes.get(ogmRelationship, Attributes.TYPE).orElseThrow();

		TypeElement ogmProperty = elements.getTypeElement(FullyQualifiedNames.OGM_PROPERTY);
		TypeElement ogmId = elements.getTypeElement(FullyQualifiedNames.OGM_ID);
		TypeElement ogmGeneratedValue = elements.getTypeElement(FullyQualifiedNames.OGM_GENERATED_VALUE);

		TypeElement ogmCompositeIndexes = elements.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEXES);
		ExecutableElement ogmCompositeIndexesValue = Attributes.get(ogmCompositeIndexes, Attributes.VALUE).orElseThrow();

		TypeElement ogmCompositeIndex = elements.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEX);
		ExecutableElement ogmCompositeIndexValue = Attributes.get(ogmCompositeIndex, Attributes.VALUE).orElseThrow();
		ExecutableElement ogmCompositeIndexProperties = Attributes.get(ogmCompositeIndex, Attributes.PROPERTIES).orElseThrow();
		ExecutableElement ogmCompositeIndexUnique = Attributes.get(ogmCompositeIndex, Attributes.UNIQUE).orElseThrow();

		TypeElement ogmIndex = elements.getTypeElement(FullyQualifiedNames.OGM_INDEX);
		ExecutableElement ogmIndexUnique = Attributes.get(ogmIndex, Attributes.UNIQUE).orElseThrow();

		TypeElement ogmRequired = elements.getTypeElement(FullyQualifiedNames.OGM_REQUIRED);

		return Optional.of(new ElementsOGM(ogmNode, ogmNodeValue, ogmNodeLabel, ogmRelationship, ogmRelationshipType, ogmRelationshipValue, ogmProperty, ogmId, ogmGeneratedValue, ogmCompositeIndexes, ogmCompositeIndexesValue, ogmCompositeIndex, ogmCompositeIndexValue, ogmCompositeIndexProperties, ogmCompositeIndexUnique, ogmIndex, ogmIndexUnique, ogmRequired));
	}
}
