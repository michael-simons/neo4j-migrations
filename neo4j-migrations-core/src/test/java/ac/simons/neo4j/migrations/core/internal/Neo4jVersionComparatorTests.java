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
package ac.simons.neo4j.migrations.core.internal;

import java.util.Comparator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Michael J. Simons
 */
class Neo4jVersionComparatorTests {

	public static final Comparator<String> VERSION_COMPARATOR = new Neo4jVersionComparator();

	@ParameterizedTest
	@CsvSource({ "Neo4j/4, Neo4j/4", "Neo4j/4, 4", "Neo4j/4, 4.0", "4.0.0, 4.0.0", "4, 4.0.0", "Neo4j/4.4-aura, 4.4",
			"4.4-aura, 4.4", "5.0.0-drop04.0, 5.0", "2025.08, 2025.08.0" })
	void equalsShouldWork(String v1, String v2) {
		Assertions.assertThat(VERSION_COMPARATOR.compare(v1, v2)).isZero();
	}

	@ParameterizedTest
	@CsvSource({ "Neo4j/3, Neo4j/4", "Neo4j/3, 4", "Neo4j/3, 4.0", "Neo4j/3, 3.1", "4.0.0, 4.0.1", "4.0.0, 4.1.0",
			"4.1.0, 4.1.1", "5.26, 2025.02.0-20138", "5.26, 2025.08", "5.0, Neo4j/2025.08.0" })
	void lessShouldWork(String v1, String v2) {
		Assertions.assertThat(VERSION_COMPARATOR.compare(v1, v2)).isNegative();
	}

	@ParameterizedTest
	@CsvSource({ "Neo4j/3, Neo4j/4", "Neo4j/3, 4", "Neo4j/3, 4.0", "Neo4j/3, 3.1", "4.0.0, 4.0.1", "4.0.0, 4.1.0",
			"4.1.0, 4.1.1", "4.4, 5", "5.26, 2025.02.0-20138", "5.26, 2025.08", "5.0, Neo4j/2025.08.0" })
	void greaterShouldWork(String v1, String v2) {
		Assertions.assertThat(VERSION_COMPARATOR.compare(v2, v1)).isPositive();
	}

}
