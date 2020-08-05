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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ContextConfiguration(initializers = MigrationsAutoConfigurationIT.Neo4jContainerBasedTestPropertyProvider.class)
class MigrationsAutoConfigurationIT {

	@Container
	private static Neo4jContainer neo4j = new Neo4jContainer<>("neo4j:4.0.0");

	private final Driver driver;

	@Autowired MigrationsAutoConfigurationIT(Driver driver) {
		this.driver = driver;
	}

	@Test
	void shouldApplyMigrations(@Autowired Migrations migrations) {

		MigrationChain migrationChain = migrations.info();

		assertThat(migrationChain.getElements()).hasSize(2);
		assertThat(migrationChain.getElements()).extracting(MigrationChain.Element::getDescription)
			.containsExactly("KeepMe", "SomeOtherMigration");

		try (Session session = driver.session()) {

			long cnt = session.run("MATCH (w:WALL) RETURN count(w)").single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	static class Neo4jContainerBasedTestPropertyProvider
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {

			TestPropertyValues.of(
				"org.neo4j.driver.uri = " + neo4j.getBoltUrl(),
				"org.neo4j.driver.authentication.username = neo4j",
				"org.neo4j.driver.authentication.password = " + neo4j.getAdminPassword(),
				"org.neo4j.migrations.packages-to-scan=ac.simons.neo4j.migrations.springframework.boot.autoconfigure.test_migrations"
			).applyTo(applicationContext.getEnvironment());
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ Neo4jDriverAutoConfiguration.class, MigrationsAutoConfiguration.class })
	static class TestConfiguration {
	}
}
