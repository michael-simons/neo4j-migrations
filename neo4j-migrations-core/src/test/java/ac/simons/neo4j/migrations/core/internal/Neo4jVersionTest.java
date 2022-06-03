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
package ac.simons.neo4j.migrations.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class Neo4jVersionTest {

	@ParameterizedTest
	@ValueSource(strings = { "4.3", "Neo4j/4.3", "4.3.4711", "Neo4j/4.3.21", "Neo4j:4.3", "neo4j:4.3.23"})
	void shouldParseDespitePrefixAndSuffix(String value) {
		Neo4jVersion version = Neo4jVersion.of(value);
		assertThat(version).isEqualTo(Neo4jVersion.V4_3);
	}
}
