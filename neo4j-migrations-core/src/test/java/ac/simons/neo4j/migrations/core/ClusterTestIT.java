/*
 * Copyright 2020-2023 the original author or authors.
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

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.junit.jupiter.causal_cluster.CausalCluster;
import org.neo4j.junit.jupiter.causal_cluster.NeedsCausalCluster;

/**
 * @author Michael J. Simons
 */
@EnabledIfEnvironmentVariable(named = "TEST_AGAINST_CC", matches = "true")
@NeedsCausalCluster(password = ClusterTestIT.PASSWORD)
class ClusterTestIT {

	static final String USERNAME = "neo4j";
	static final String PASSWORD = "cc";

	@CausalCluster // (2)
	private static URI neo4jUri;

	private static Driver driver;

	@BeforeAll
	static void initDriver() {
		Config config = Config.builder().withLogging(Logging.none()).build();
		driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(USERNAME, PASSWORD), config);
	}

	@AfterAll
	static void closeDriver() {

		driver.close();
	}

	@Test
	void shouldWorkAgainstCC() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1").build(), driver);
		migrations.apply();

		assertThat(TestBase.lengthOfMigrations(driver, null)).isEqualTo(2);

		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();

		assertThat(TestBase.lengthOfMigrations(driver, null)).isEqualTo(5);

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements())
			.hasSizeGreaterThan(0)
			.allMatch(element -> element.getState() == MigrationState.APPLIED);
	}
}
