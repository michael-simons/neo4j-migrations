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

/**
 * Constants for various external annotations. We decided to not depend on the actual
 * classes in the processor to keep its dependencies as small as possible.
 *
 * @author Michael J. Simons
 * @since 1.11.0
 */
final class FullyQualifiedNames {

	static final String OGM_ID = "org.neo4j.ogm.annotation.Id";
	static final String OGM_NODE = "org.neo4j.ogm.annotation.NodeEntity";
	static final String OGM_RELATIONSHIP_ENTITY = "org.neo4j.ogm.annotation.RelationshipEntity";
	static final String OGM_INDEX = "org.neo4j.ogm.annotation.Index";
	static final String OGM_COMPOSITE_INDEX = "org.neo4j.ogm.annotation.CompositeIndex";
	static final String OGM_COMPOSITE_INDEXES = "org.neo4j.ogm.annotation.CompositeIndexes";
	static final String OGM_REQUIRED = "org.neo4j.ogm.annotation.Required";
	static final String OGM_GENERATED_VALUE = "org.neo4j.ogm.annotation.GeneratedValue";
	static final String OGM_PROPERTY = "org.neo4j.ogm.annotation.Property";
	static final String OGM_TRANSIENT = "org.neo4j.ogm.annotation.Transient";

	static final String SDN6_NODE = "org.springframework.data.neo4j.core.schema.Node";
	static final String SDN6_ID = "org.springframework.data.neo4j.core.schema.Id";
	static final String SDN6_GENERATED_VALUE = "org.springframework.data.neo4j.core.schema.GeneratedValue";
	static final String SDN6_RELATIONSHIP_PROPERTIES = "org.springframework.data.neo4j.core.schema.RelationshipProperties";
	static final String SDN6_PROPERTY = "org.springframework.data.neo4j.core.schema.Property";

	static final String COMMONS_ID = "org.springframework.data.annotation.Id";
	static final String COMMONS_TRANSIENT = "org.springframework.data.annotation.Transient";

	static final String CATALOG_REQUIRED = "ac.simons.neo4j.migrations.annotations.catalog.Required";
	static final String CATALOG_UNIQUE = "ac.simons.neo4j.migrations.annotations.catalog.Unique";
	static final String CATALOG_UNIQUE_PROPERTIES = "ac.simons.neo4j.migrations.annotations.catalog.UniqueProperties";
	static final String CATALOG_INDEX = "ac.simons.neo4j.migrations.annotations.catalog.Index";
	static final String CATALOG_INDEXES = "ac.simons.neo4j.migrations.annotations.catalog.Indexes";

	private FullyQualifiedNames() {
	}

}
