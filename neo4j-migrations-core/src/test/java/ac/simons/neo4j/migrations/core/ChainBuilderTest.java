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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class ChainBuilderTest extends TestBase {

	@Test
	void migrationInfoShouldWork() {

		// Apply some migrations
		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1").build(), driver);
		migrations.apply();

		// Now use only the chain service
		MigrationsConfig config = MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
			"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:my/awesome/migrations/moreStuff")
			.build();
		MigrationContext context = new Migrations.DefaultMigrationContext(config, driver);
		MigrationChain migrationChain = new ChainBuilder()
			.buildChain(context, new DiscoveryService().findMigrations(context));

		assertThat(migrationChain.getServerAddress()).isEqualTo(getServerAddress());
		assertThat(migrationChain.getServerVersion()).isEqualTo("Neo4j/4.0.0");
		assertThat(migrationChain.getUsername()).isEqualTo("neo4j");
		assertThat(migrationChain.getDatabaseName()).isEqualTo("neo4j");
		assertThat(migrationChain.getElements()).hasSize(4);
		assertThat(migrationChain.getElements()).element(0).satisfies(element -> {

			assertThat(element.getState()).isEqualTo(MigrationState.APPLIED);
			assertThat(element.getType()).isEqualTo(MigrationType.JAVA);
			assertThat(element.getChecksum()).isEmpty();
			assertThat(element.getVersion()).isEqualTo("001");
			assertThat(element.getDescription()).isEqualTo("FirstMigration");
			assertThat(element.getSource())
				.isEqualTo("ac.simons.neo4j.migrations.core.test_migrations.changeset1.V001__FirstMigration");
			assertThat(element.getInstalledOn()).isPresent();
			assertThat(element.getInstalledBy()).hasValue(System.getProperty("user.name") + "/neo4j");
			assertThat(element.getExecutionTime()).isPresent();
		});
		assertThat(migrationChain.getElements()).element(1).satisfies(element -> {

			assertThat(element.getState()).isEqualTo(MigrationState.APPLIED);
			assertThat(element.getType()).isEqualTo(MigrationType.JAVA);
			assertThat(element.getChecksum()).isEmpty();
			assertThat(element.getVersion()).isEqualTo("002");
			assertThat(element.getDescription()).isEqualTo("AnotherMigration");
			assertThat(element.getSource())
				.isEqualTo("ac.simons.neo4j.migrations.core.test_migrations.changeset1.V002__AnotherMigration");
			assertThat(element.getInstalledOn()).isPresent();
			assertThat(element.getInstalledBy()).hasValue(System.getProperty("user.name") + "/neo4j");
			assertThat(element.getExecutionTime()).isPresent();
		});
		assertThat(migrationChain.getElements()).element(2).satisfies(element -> {

			assertThat(element.getState()).isEqualTo(MigrationState.PENDING);
			assertThat(element.getType()).isEqualTo(MigrationType.CYPHER);
			assertThat(element.getChecksum()).isPresent();
			assertThat(element.getVersion()).isEqualTo("007");
			assertThat(element.getDescription()).isEqualTo("BondTheNameIsBond");
			assertThat(element.getSource()).isEqualTo("V007__BondTheNameIsBond.cypher");
			assertThat(element.getInstalledOn()).isEmpty();
			assertThat(element.getInstalledBy()).isEmpty();
			assertThat(element.getExecutionTime()).isEmpty();
		});
		assertThat(migrationChain.getElements()).element(3).satisfies(element -> {

			assertThat(element.getState()).isEqualTo(MigrationState.PENDING);
			assertThat(element.getType()).isEqualTo(MigrationType.JAVA);
			assertThat(element.getChecksum()).isEmpty();
			assertThat(element.getVersion()).isEqualTo("023");
			assertThat(element.getDescription()).isEqualTo("NichtsIstWieEsScheint");
			assertThat(element.getSource())
				.isEqualTo("ac.simons.neo4j.migrations.core.test_migrations.changeset2.V023__NichtsIstWieEsScheint");
			assertThat(element.getInstalledOn()).isEmpty();
			assertThat(element.getInstalledBy()).isEmpty();
			assertThat(element.getExecutionTime()).isEmpty();
		});
	}
}
