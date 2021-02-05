/*
 * Copyright 2020 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Migrations.DefaultMigrationContext;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 */
class DiscoveryServiceTest {

	@Test
	void shouldDiscoverMigrationsInCorrectOrder() {

		MigrationContext context = new DefaultMigrationContext(MigrationsConfig.builder()
			.withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset3",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:/my/awesome/migrations")
			.build(), Mockito.mock(Driver.class));

		List<Migration> migrations = new DiscoveryService().findMigrations(context);
		assertThat(migrations).hasSize(13)
			.extracting(Migration::getDescription)
			.containsExactly("FirstMigration", "AnotherMigration", "InnerMigration", "BondTheNameIsBond",
				"BondTheNameIsBondNew", "BondTheNameIsBondNewNew", "Die halbe Wahrheit", "Die halbe Wahrheit neu",
					"Die halbe Wahrheit neu neu", "NichtsIstWieEsScheint", "NichtsIstWieEsScheintNeu", "NichtsIstWieEsScheintNeuNeu",
					"MirFallenKeineNamenEin");
	}
}
