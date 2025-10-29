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
package ac.simons.neo4j.migrations.cluster_tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.MigrationState;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import com.sun.security.auth.module.UnixSystem;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Only run if needed")
class ClusterTestIT {

	static final String USERNAME = "neo4j";
	static final String PASSWORD = "verysecret";

	protected static final UnixSystem unixSystem = new UnixSystem();

	@SuppressWarnings("resource")
	@Container
	protected static final ComposeContainer environment =
		new ComposeContainer(new File("src/test/resources/cc/docker-compose.yml"))
			.withEnv(Map.of("USER_ID", Long.toString(unixSystem.getUid()), "GROUP_ID", Long.toString(unixSystem.getGid())))
			.withExposedService("server1", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
			.withExposedService("server2", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
			.withExposedService("server3", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
			.withExposedService("server4", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

	private static Driver driver;

	@BeforeAll
	static void initDriver() {
		Config config = Config.builder().withLogging(Logging.none()).build();
		driver = GraphDatabase.driver("neo4j://localhost:%d".formatted(environment.getServicePort("server1", 7687)), AuthTokens.basic(USERNAME, PASSWORD), config);
	}

	@AfterAll
	static void closeDriver() {

		driver.close();
	}

	@Test
	void shouldWorkAgainstCC() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan("/change1").build(), driver);
		migrations.apply();

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements()).hasSize(2);

		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan("/change1", "/change2").build(), driver);
		migrations.apply();

		migrationChain = migrations.info();
		assertThat(migrationChain.getElements())
			.hasSize(5)
			.allMatch(element -> element.getState() == MigrationState.APPLIED);

		try (var session = driver.session()) {
			var cnt = session.executeRead(tx -> tx.run("MATCH (n:Node) WHERE n.name IN ['M1', 'M2', 'M3', 'M4', 'M5'] RETURN count(n)").single().get(0)).asLong();
			assertThat(cnt).isEqualTo(5);
		}
	}
}
