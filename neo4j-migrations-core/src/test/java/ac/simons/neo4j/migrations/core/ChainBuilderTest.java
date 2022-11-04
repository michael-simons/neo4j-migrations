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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.test_resources.TestResources;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class ChainBuilderTest {

	@Test
	void shouldMatch() {

		CypherBasedMigration cypherBasedMigration = new CypherBasedMigration(
			TestResources.class.getResource("/my/awesome/migrations/V021__Die halbe Wahrheit.cypher"));
		cypherBasedMigration.setAlternativeChecksums(Collections.singletonList("foobar"));

		assertThat(ChainBuilder.matches(Optional.of("200310393"), cypherBasedMigration)).isTrue();
		assertThat(ChainBuilder.matches(Optional.of("whatever"), (JavaBasedMigration) context -> {
		})).isFalse();
		assertThat(ChainBuilder.matches(Optional.empty(), cypherBasedMigration)).isFalse();
		assertThat(ChainBuilder.matches(Optional.of("foobar"), cypherBasedMigration)).isTrue();
	}
}
