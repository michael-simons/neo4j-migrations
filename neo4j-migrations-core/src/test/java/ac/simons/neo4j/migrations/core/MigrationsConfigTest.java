/*
 * Copyright 2020-2021 the original author or authors.
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
class MigrationsConfigTest {

	@Test
	void shouldConfigureDefaultClasspathPackage() {

		assertThat(MigrationsConfig.builder().build().getLocationsToScan()).isEqualTo(Defaults.LOCATIONS_TO_SCAN);
	}

	@Test // GH-237
	void validateOnInstallShouldBeTrueByDefault() {

		assertThat(MigrationsConfig.builder().build().isValidateOnMigrate()).isTrue();
	}

	@Test // GH-237
	void validateOnInstallShouldBeChangeable() {

		assertThat(MigrationsConfig.builder().withValidateOnMigrate(false).build().isValidateOnMigrate()).isFalse();
	}
}
