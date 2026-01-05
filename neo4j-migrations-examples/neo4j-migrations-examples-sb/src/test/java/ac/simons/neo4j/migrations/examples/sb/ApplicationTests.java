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
package ac.simons.neo4j.migrations.examples.sb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Michael J. Simons
 */
class ApplicationTests {

	// Yes, I do know about @SpringBootTest and @TestConfiguration, but after renaming the
	// package of those examples,
	// this has been the easiest way to convince Sonar cloud that they are actually
	// covered by tests, and I have done
	// enough Maven madness already
	@Test
	void contextLoads() {

		assertThatNoException().isThrownBy(
				() -> Application.main("--org.neo4j.migrations.enabled=false", "--spring.main.banner-mode=off"));

		// Same here, Person is actually needed by the annotation processor but JaCoCo and
		// Sonar are stupid.
		var person = new Person("Test");
		assertThat(person.getId()).isNull();
		assertThat(person.getName()).isEqualTo("Test");
	}

}
