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
package ac.simons.neo4j.migrations.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class RepairementResultTests {

	@Test
	void shouldNotPrintThingsWhenNothingHappenedInDefaultDb() {
		var result = RepairmentResult.unnecessary(null);
		assertThat(result.prettyPrint())
			.isEqualTo("The default database is in a valid state, missing or new migrations can be applied");
	}

	@Test
	void shouldNotPrintThingsWhenNothingHappenedInSpecificDb() {
		var result = RepairmentResult.unnecessary("foobar");
		assertThat(result.prettyPrint())
			.isEqualTo("`foobar` is in a valid state, missing or new migrations can be applied");
	}

	@Test
	void shouldFormatInDefaultDb() {
		var result = RepairmentResult.repaired(null, 1, 3, 2, 4, 5);
		assertThat(result.prettyPrint()).isEqualTo(
				"The migration chain in the default database has been repaired: 1 nodes and 2 relationships have been deleted, 3 nodes and 4 relationships have been recreated.");
	}

	@Test
	void shouldFormatInSpecificDb() {
		var result = RepairmentResult.repaired("foobar", 1, 3, 2, 4, 5);
		assertThat(result.prettyPrint()).isEqualTo(
				"The migration chain in `foobar` has been repaired: 1 nodes and 2 relationships have been deleted, 3 nodes and 4 relationships have been recreated.");
	}

}
