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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ac.simons.neo4j.migrations.annotations.proc.RelationshipType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;

/**
 * @author Michael J. Simons
 */
class DefaultRelationshipTypeTest {

	@Test
	void addPropertyShouldWork() {

		var defaultRelationshipType = new DefaultRelationshipType("Foo", DefaultSchemaName.type("FOO"));
		var property = defaultRelationshipType.addProperty("x");
		assertThat(property.getName()).isEqualTo("x");
		assertThat(property.getOwner()).extracting(RelationshipType::getName)
			.extracting(SchemaName::getValue)
			.isEqualTo("FOO");
	}
}
