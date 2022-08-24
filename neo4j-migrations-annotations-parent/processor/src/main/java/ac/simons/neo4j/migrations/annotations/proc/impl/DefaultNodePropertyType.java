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

import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;

/**
 * @author Michael J. Simons
 * @soundtrack Ralf "Ralle" Petersen -  Album wird aus Hack gemacht 2016
 * @since TBA
 */
final class DefaultNodePropertyType implements PropertyType<NodeType> {

	private final NodeType owner;

	private final String name;

	DefaultNodePropertyType(NodeType owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	@Override
	public NodeType getOwner() {
		return this.owner;
	}

	@Override
	public String getName() {
		return null;
	}
}
