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

import java.util.Collections;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class DefaultConstraintNameGeneratorTests {

	@Test
	void shouldGenerateExpectedName() {
		ConstraintNameGenerator generator = new DefaultConstraintNameGenerator();

		NodeType nodeType = mock(NodeType.class);
		given(nodeType.getOwningTypeName()).willReturn("this.is.a.Test");
		@SuppressWarnings("unchecked")
		PropertyType<NodeType> property = mock(PropertyType.class);
		given(property.getOwner()).willReturn(nodeType);
		given(property.getName()).willReturn("id");

		String name = generator.generateName(Constraint.Type.UNIQUE, Collections.singleton(property));
		assertThat(name).isEqualTo("this_is_a_test_id_unique");
	}

}
