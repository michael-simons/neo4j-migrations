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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;

/**
 * Default {@link NodeType}.
 *
 * @author Michael J. Simons
 * @since 1.11.0
 */
final class DefaultNodeType implements NodeType, WriteableElementType<NodeType> {

	private final String owningTypeName;

	private final List<SchemaName> names;

	private final List<PropertyType<NodeType>> properties;

	DefaultNodeType(String owningTypeName, List<SchemaName> names) {
		this.owningTypeName = owningTypeName;
		this.names = names;
		this.properties = new ArrayList<>();
	}

	@Override
	public String getOwningTypeName() {
		return this.owningTypeName;
	}

	@Override
	public List<PropertyType<NodeType>> getProperties() {
		return Collections.unmodifiableList(this.properties);
	}

	@Override
	public List<SchemaName> getLabels() {
		return Collections.unmodifiableList(this.names);
	}

	@Override
	public PropertyType<NodeType> addProperty(String property) {
		PropertyType<NodeType> propertyType = new DefaultPropertyType<>(this, property);
		this.properties.add(propertyType);
		return propertyType;
	}

}
