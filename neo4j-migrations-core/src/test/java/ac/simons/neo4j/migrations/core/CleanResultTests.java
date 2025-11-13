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
package ac.simons.neo4j.migrations.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class CleanResultTests {

	@Test
	void shouldHaveNiceSentenceWithoutChain() {
		CleanResult result = new CleanResult(Optional.empty(), Collections.emptyList(), 0, 0, 0, 0);
		assertThat(result.prettyPrint()).isEqualTo(
				"Deleted no chains (0 nodes and 0 relationships in total) and 0 constraints from the default database.");
	}

	@Test
	void shouldJoinChains() {
		CleanResult result = new CleanResult(Optional.empty(), Arrays.asList("a", "b"), 0, 0, 0, 0);
		assertThat(result.prettyPrint()).isEqualTo(
				"Deleted chains a, b (0 nodes and 0 relationships in total) and 0 constraints from the default database.");
	}

	@Test
	void shouldFormatDatabase() {
		CleanResult result = new CleanResult(Optional.of("x"), Arrays.asList("a"), 0, 0, 0, 0);
		assertThat(result.prettyPrint())
			.isEqualTo("Deleted chain a (0 nodes and 0 relationships in total) and 0 constraints from `x`.");
	}

	@Test
	void shouldUseCorrectNumbers() {
		CleanResult result = new CleanResult(Optional.of("x"), Arrays.asList("a"), 1, 2, 3, 4);
		assertThat(result.prettyPrint())
			.isEqualTo("Deleted chain a (1 nodes and 2 relationships in total) and 3 constraints from `x`.");
	}

}
