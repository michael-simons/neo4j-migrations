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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;

/**
 * @author Michael J. Simons
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MigrationsAutoConfigurationIT {

	static {

		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
	}

	@Container
	private static final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("migrations.default-neo4j-image"));

	private final Driver driver;

	@Autowired MigrationsAutoConfigurationIT(Driver driver) {
		this.driver = driver;
	}

	@Test
	void shouldApplyMigrations(@Autowired Migrations migrations, @Value("${spring.application.name}") String appName) {

		MigrationChain migrationChain = migrations.info();

		assertThat(migrationChain.getElements())
			.extracting(MigrationChain.Element::getOptionalDescription)
			.filteredOn(Optional::isPresent)
			.extracting(Optional::get)
			.hasSize(3)
			.containsExactly("KeepMe", "SomeOtherMigration", "AFreshSpringMigration");

		try (Session session = driver.session()) {

			long cnt = session.run("MATCH (w:WALL) RETURN count(w)").single().get(0).asLong();
			assertThat(cnt).isEqualTo(2L);

			cnt = session.run("MATCH (w:WALL) WHERE w.appName = $appName RETURN count(w)", Map.of("appName", appName)).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", () -> "password");
		registry.add("org.neo4j.migrations.packages-to-scan", () -> "ac.simons.neo4j.migrations.springframework.boot.autoconfigure.test_migrations");
		registry.add("spring.application.name", () -> "MigrationIT");
	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan("ac.simons.neo4j.migrations.springframework.boot.autoconfigure.test_migrations")
	@ImportAutoConfiguration({ Neo4jAutoConfiguration.class, MigrationsAutoConfiguration.class })
	static class TestConfiguration {
	}
}
