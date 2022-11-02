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

/**
 * @author Michael J. Simons
 */
class MigrationOptionsTest {

	@Test
	void shouldNotFailWithGarbageString() {

		MigrationOptions options = MigrationOptions.fromPathOrUrl("whatever");

		assertThat(options.isRunAlways()).isFalse();
		assertThat(options.isRunOnChange()).isFalse();
	}

	@Test
	void defaultsShouldBeAsPriorToRunAlwaysFeature() {

		MigrationOptions options = MigrationOptions.defaults();

		assertThat(options.isRunAlways()).isFalse();
		assertThat(options.isRunOnChange()).isFalse();
	}

	@Test
	void shouldNotFailWithoutOptions() {

		MigrationOptions options = MigrationOptions.fromPathOrUrl("V1.2.3__Whatever.cypher");

		assertThat(options.isRunAlways()).isFalse();
		assertThat(options.isRunOnChange()).isFalse();
	}

	@Test
	void runAlwaysShouldWork() {

		MigrationOptions options = MigrationOptions.fromPathOrUrl("V1.2.3__Whatever(runAlways).cypher");

		assertThat(options.isRunAlways()).isTrue();
		assertThat(options.isRunOnChange()).isFalse();
	}

	@Test
	void runOnChangeShouldWork() {

		MigrationOptions options = MigrationOptions.fromPathOrUrl("V1.2.3__Whatever(runOnChange).cypher");

		assertThat(options.isRunAlways()).isFalse();
		assertThat(options.isRunOnChange()).isTrue();
	}
}
