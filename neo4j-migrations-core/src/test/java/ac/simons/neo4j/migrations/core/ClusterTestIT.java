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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@EnabledOnOs(OS.LINUX)
class ClusterTestIT {

	static final String USERNAME = "neo4j";
	static final String PASSWORD = "verysecret";

	@SuppressWarnings("resource")
	@Container
	protected static final ComposeContainer environment =
		new ComposeContainer(new File("src/test/resources/cc/docker-compose.yml"))
			.withEnv(Map.of("USER_ID", userIdentity("u"), "GROUP_ID", userIdentity("g")))
			.withExposedService("server1", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
			.withLocalCompose(true);

	private static Driver driver;

	private static String userIdentity(String what) {
		try (var s = new ProcessBuilder().command("id", "-" + what).start().getInputStream()) {
			return new String(s.readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

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
