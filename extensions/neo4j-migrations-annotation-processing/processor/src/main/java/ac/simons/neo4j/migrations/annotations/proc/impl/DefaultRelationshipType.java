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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.annotations.proc.RelationshipType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;

/**
 * Default {@link RelationshipType}.
 *
 * @author Michael J. Simons
 * @since 1.11.0
 */
final class DefaultRelationshipType implements RelationshipType, WriteableElementType<RelationshipType> {

	private final String owningTypeName;

	private final SchemaName name;

	private final List<PropertyType<RelationshipType>> properties;

	DefaultRelationshipType(String owningTypeName, SchemaName name) {
		this.owningTypeName = owningTypeName;
		this.name = name;
		this.properties = new ArrayList<>();
	}

	@Override
	public String getOwningTypeName() {
		return this.owningTypeName;
	}

	@Override
	public List<PropertyType<RelationshipType>> getProperties() {
		return Collections.unmodifiableList(this.properties);
	}

	@Override
	public SchemaName getName() {
		return this.name;
	}

	@Override
	public PropertyType<RelationshipType> addProperty(String property) {
		PropertyType<RelationshipType> propertyType = new DefaultPropertyType<>(this, property);
		this.properties.add(propertyType);
		return propertyType;
	}

}
