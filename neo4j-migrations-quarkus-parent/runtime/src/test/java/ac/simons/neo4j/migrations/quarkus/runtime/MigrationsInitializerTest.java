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
package ac.simons.neo4j.migrations.quarkus.runtime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

/**
 * I haven't managed to make JaCoCo it's under test otherwise
 *
 * @author Michael J. Simons
 */
class MigrationsInitializerTest {

	@Test
	void disablingShouldWork() {

		MigrationsInitializer initializer = new MigrationsInitializer(null, new MigrationsEnabled(false));
		Assertions.assertThatNoException().isThrownBy(() -> initializer.onStart(null));
	}

	@Test
	void shouldNotThrowWithoutConnection() {

		Driver driver = mock(Driver.class);

		doThrow(ServiceUnavailableException.class).when(driver).verifyConnectivity();
		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		MigrationsInitializer initializer = new MigrationsInitializer(migrations, new MigrationsEnabled(true));
		Assertions.assertThatNoException().isThrownBy(() -> initializer.onStart(null));
	}
}
