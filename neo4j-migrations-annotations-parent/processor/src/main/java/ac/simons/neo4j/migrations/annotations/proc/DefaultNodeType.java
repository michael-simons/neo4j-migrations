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
package ac.simons.neo4j.migrations.annotations.proc;

import ac.simons.neo4j.migrations.schema.Label;
import ac.simons.neo4j.migrations.schema.NodeType;
import ac.simons.neo4j.migrations.schema.PropertyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael J. Simons
 * @soundtrack Ralf "Ralle" Petersen -  Album wird aus Hack gemacht 2016
 * @since TBA
 */
final class DefaultNodeType implements NodeType {

	private final List<Label> labels;

	private final List<PropertyType<NodeType>> properties;

	DefaultNodeType(List<Label> labels) {
		this.labels = labels;
		this.properties = new ArrayList<>();
		// TODO BIDI prop
	}

	@Override
	public List<PropertyType<NodeType>> getProperties() {
		return Collections.unmodifiableList(this.properties);
	}

	@Override
	public List<Label> getLabels() {
		return Collections.unmodifiableList(this.labels);
	}
}
