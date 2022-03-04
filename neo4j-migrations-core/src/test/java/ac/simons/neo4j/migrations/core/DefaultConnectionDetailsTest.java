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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Michael J. Simons
 */
class DefaultConnectionDetailsTest {

	@Test
	void shouldCreateVersionAndEditionWithoutEdition() {
		String versionAndEdition = DefaultConnectionDetails.createVersionAndEdition("Neo4j/4711", null);
		assertThat(versionAndEdition).isEqualTo("Neo4j/4711");
	}

	@Test
	void shouldCreateVersionAndEditionWithEdition() {
		String versionAndEdition = DefaultConnectionDetails.createVersionAndEdition("Neo4j/4711", "special");
		assertThat(versionAndEdition).isEqualTo("Neo4j/4711 Special edition");
	}

	@ParameterizedTest
	@CsvSource({ "community, COMMUNITY", "enterprise, ENTERPRISE", ", UNKNOWN", "special, UNKNOWN" })
	void editionShouldBeDetecable(String value, HBD.Edition edition) {
		assertThat(HBD.getEdition(new DefaultConnectionDetails(null, "Neo4j/4711", value, null, null, null)))
			.isEqualTo(edition);
	}
}
