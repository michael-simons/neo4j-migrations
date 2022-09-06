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

/**
 * Constants for various external annotations. We decided to not depend on the actual classes in the processor to keep its
 * dependencies as small as possible.
 *
 * @author Michael J. Simons
 * @soundtrack Moonbootica - ...And Then We Started To Dance
 * @since TBA
 */
final class FullyQualifiedNames {

	static final String OGM_NODE = "org.neo4j.ogm.annotation.NodeEntity";
	static final String OGM_INDEX = "org.neo4j.ogm.annotation.Index";
	static final String OGM_COMPOSITE_INDEX = "org.neo4j.ogm.annotation.CompositeIndex";
	static final String OGM_COMPOSITE_INDEXES = "org.neo4j.ogm.annotation.CompositeIndexes";
	static final String OGM_REQUIRED = "org.neo4j.ogm.annotation.Required";

	static final String SDN6_NODE = "org.springframework.data.neo4j.core.schema.Node";
	static final String SDN6_ID = "org.springframework.data.neo4j.core.schema.Id";
	static final String SDN6_GENERATED_VALUE = "org.springframework.data.neo4j.core.schema.GeneratedValue";

	static final String COMMONS_ID = "org.springframework.data.annotation.Id";

	private FullyQualifiedNames() {
	}
}
