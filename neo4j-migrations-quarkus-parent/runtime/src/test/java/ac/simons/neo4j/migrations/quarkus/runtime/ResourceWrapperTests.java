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
package ac.simons.neo4j.migrations.quarkus.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Michael J. Simons
 */
class ResourceWrapperTests {

	@Test
	void shouldCheckNull() {
		var wrapper = new ResourceWrapper();

		assertThatNullPointerException().isThrownBy(() -> wrapper.setUrl(null));
		assertThatNullPointerException().isThrownBy(() -> wrapper.setPath(null));
	}

	@Test
	void equalsShouldWork() {

		var wrapper1 = new ResourceWrapper();
		wrapper1.setPath("p");
		wrapper1.setUrl("u");

		var wrapper2 = new ResourceWrapper();
		wrapper2.setPath(wrapper1.getPath());
		wrapper2.setUrl(wrapper1.getUrl());

		assertThat(wrapper1).isEqualTo(wrapper2).hasSameHashCodeAs(wrapper2);
	}

}
