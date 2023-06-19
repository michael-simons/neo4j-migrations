/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class GeneratedNameTest {

	@Test
	void idGenerationShouldWork() {

		Constraint constraint = new Constraint(Constraint.Type.KEY, TargetEntityType.NODE, "Person",
			Arrays.asList("firstname", "surname"), null);
		Name name = constraint.getName();
		Assertions.assertThat(name).isInstanceOf(GeneratedName.class).extracting(Name::getValue)
			.isEqualTo("Constraint_4006808BBCCF49878FDFA78BF2148FAD");

		Constraint constraint2 = new Constraint(Constraint.Type.KEY, TargetEntityType.NODE, "Person",
			Arrays.asList("firstname", "surname"), null);
		Assertions.assertThat(constraint2.getName()).isEqualTo(name);
	}
}
