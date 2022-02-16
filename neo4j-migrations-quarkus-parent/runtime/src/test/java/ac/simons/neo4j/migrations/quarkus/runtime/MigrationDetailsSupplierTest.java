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

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 */
class MigrationDetailsSupplierTest {

	@Test
	void shouldSupplyProvidedValue() {

		var supplier = new MigrationDetailsSupplier();
		var migrations = new Migrations(MigrationsConfig.defaultConfig(), Mockito.mock(Driver.class));
		supplier.setValue(migrations);
		assertThat(supplier.get()).isEqualTo(migrations);
	}
}
