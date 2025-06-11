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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.NoSuchElementException;
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
final class ElementsOGM {

	static Optional<ElementsOGM> of(Elements elements) {
		TypeElement ogmNode = elements.getTypeElement(FullyQualifiedNames.OGM_NODE);
		if (ogmNode == null) {
			return Optional.empty();
		}

		ExecutableElement ogmNodeValue = Attributes.get(ogmNode, Attributes.VALUE).orElseThrow(NoSuchElementException::new);
		ExecutableElement ogmNodeLabel = Attributes.get(ogmNode, Attributes.LABEL).orElseThrow(NoSuchElementException::new);

		TypeElement ogmRelationship = elements.getTypeElement(FullyQualifiedNames.OGM_RELATIONSHIP);
		ExecutableElement ogmRelationshipValue = Attributes.get(ogmRelationship, Attributes.VALUE).orElseThrow(NoSuchElementException::new);
		ExecutableElement ogmRelationshipType = Attributes.get(ogmRelationship, Attributes.TYPE).orElseThrow(NoSuchElementException::new);

		TypeElement ogmProperty = elements.getTypeElement(FullyQualifiedNames.OGM_PROPERTY);
		TypeElement ogmId = elements.getTypeElement(FullyQualifiedNames.OGM_ID);
		TypeElement ogmGeneratedValue = elements.getTypeElement(FullyQualifiedNames.OGM_GENERATED_VALUE);

		TypeElement ogmCompositeIndexes = elements.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEXES);
		ExecutableElement ogmCompositeIndexesValue = Attributes.get(ogmCompositeIndexes, Attributes.VALUE).orElseThrow(NoSuchElementException::new);

		TypeElement ogmCompositeIndex = elements.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEX);
		ExecutableElement ogmCompositeIndexValue = Attributes.get(ogmCompositeIndex, Attributes.VALUE).orElseThrow(NoSuchElementException::new);
		ExecutableElement ogmCompositeIndexProperties = Attributes.get(ogmCompositeIndex, Attributes.PROPERTIES).orElseThrow(NoSuchElementException::new);
		ExecutableElement ogmCompositeIndexUnique = Attributes.get(ogmCompositeIndex, Attributes.UNIQUE).orElseThrow(NoSuchElementException::new);

		TypeElement ogmIndex = elements.getTypeElement(FullyQualifiedNames.OGM_INDEX);
		ExecutableElement ogmIndexUnique = Attributes.get(ogmIndex, Attributes.UNIQUE).orElseThrow(NoSuchElementException::new);

		TypeElement ogmRequired = elements.getTypeElement(FullyQualifiedNames.OGM_REQUIRED);

		return Optional.of(new ElementsOGM(ogmNode, ogmNodeValue, ogmNodeLabel, ogmRelationship, ogmRelationshipType, ogmRelationshipValue, ogmProperty, ogmId, ogmGeneratedValue, ogmCompositeIndexes, ogmCompositeIndexesValue, ogmCompositeIndex, ogmCompositeIndexValue, ogmCompositeIndexProperties, ogmCompositeIndexUnique, ogmIndex, ogmIndexUnique, ogmRequired));
	}

	private final TypeElement node;
	private final ExecutableElement nodeValue;
	private final ExecutableElement nodeLabel;
	private final TypeElement relationship;
	private final ExecutableElement relationshipType;
	private final ExecutableElement relationshipValue;
	private final TypeElement property;
	private final TypeElement id;
	private final TypeElement generatedValue;
	private final TypeElement compositeIndexes;
	private final ExecutableElement compositeIndexesValue;
	private final TypeElement compositeIndex;
	private final ExecutableElement compositeIndexValue;
	private final ExecutableElement compositeIndexProperties;
	private final ExecutableElement compositeIndexUnique;
	private final TypeElement index;
	private final ExecutableElement indexUnique;

	private final TypeElement required;

	@SuppressWarnings("squid:S107") // That's what you get for back-porting a record to JDK 8
	private ElementsOGM(TypeElement node, ExecutableElement nodeValue, ExecutableElement nodeLabel,
		TypeElement relationship, ExecutableElement relationshipType,
		ExecutableElement relationshipValue, TypeElement property, TypeElement id,
		TypeElement generatedValue, TypeElement compositeIndexes,
		ExecutableElement compositeIndexesValue, TypeElement compositeIndex,
		ExecutableElement compositeIndexValue, ExecutableElement compositeIndexProperties,
		ExecutableElement compositeIndexUnique, TypeElement index, ExecutableElement indexUnique,
		TypeElement required
	) {
		this.node = node;
		this.nodeValue = nodeValue;
		this.nodeLabel = nodeLabel;
		this.relationship = relationship;
		this.relationshipType = relationshipType;
		this.relationshipValue = relationshipValue;
		this.property = property;
		this.id = id;
		this.generatedValue = generatedValue;
		this.compositeIndexes = compositeIndexes;
		this.compositeIndexesValue = compositeIndexesValue;
		this.compositeIndex = compositeIndex;
		this.compositeIndexValue = compositeIndexValue;
		this.compositeIndexProperties = compositeIndexProperties;
		this.compositeIndexUnique = compositeIndexUnique;
		this.index = index;
		this.indexUnique = indexUnique;
		this.required = required;
	}

	public TypeElement node() {
		return node;
	}

	public ExecutableElement nodeValue() {
		return nodeValue;
	}

	public ExecutableElement nodeLabel() {
		return nodeLabel;
	}

	public TypeElement relationship() {
		return relationship;
	}

	public ExecutableElement relationshipType() {
		return relationshipType;
	}

	public ExecutableElement relationshipValue() {
		return relationshipValue;
	}

	public TypeElement property() {
		return property;
	}

	public TypeElement id() {
		return id;
	}

	public TypeElement generatedValue() {
		return generatedValue;
	}

	public TypeElement compositeIndexes() {
		return compositeIndexes;
	}

	public ExecutableElement compositeIndexesValue() {
		return compositeIndexesValue;
	}

	public TypeElement compositeIndex() {
		return compositeIndex;
	}

	public ExecutableElement compositeIndexValue() {
		return compositeIndexValue;
	}

	public ExecutableElement compositeIndexProperties() {
		return compositeIndexProperties;
	}

	public ExecutableElement compositeIndexUnique() {
		return compositeIndexUnique;
	}

	public TypeElement index() {
		return index;
	}

	public ExecutableElement indexUnique() {
		return indexUnique;
	}

	public TypeElement required() {
		return required;
	}
}
