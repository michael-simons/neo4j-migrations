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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Very specific tests for detecting capabilities and error conditions.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SkipArm64IncompatibleConfiguration.class)
class EnterpriseRequiredDetectionIT {

	@ParameterizedTest // GH-647
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldFailAsGracefullyAsItGetsWhenEditionMismatch(
			SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		Neo4jContainer neo4j = new Neo4jContainer(String.format("neo4j:%s", version.value.toString()))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.withReuse(true);
		neo4j.start();

		Constraint[] exists = new Constraint[] { Constraint.forNode("Book").named("x").exists("isbn"),
				Constraint.forNode("Book").named("y").key("title"),
				Constraint.forRelationship("PUBLISHED").named("z").exists("on") };
		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		// Force the renderer to enterprise
		RenderConfig cfg = RenderConfig.create()
			.forVersionAndEdition(version.value, Neo4jEdition.ENTERPRISE)
			.ignoreName();

		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()))) {
			MigrationsConfig configuration = MigrationsConfig.builder()
				.withLocationsToScan("classpath:ee")
				.withAutocrlf(true)
				.build();
			Migrations migrations = new Migrations(configuration, driver);
			ConnectionDetails connectionDetails = migrations.getConnectionDetails();

			for (Constraint constraint : exists) {
				String statement = renderer.render(constraint, cfg);
				try (Session session = driver.session()) {
					session.run(statement);
					Assertions.fail("An exception was expected");
				}
				catch (Neo4jException ex) {
					assertThat(HBD.constraintProbablyRequiredEnterpriseEdition(ex, connectionDetails)).isTrue();
				}
			}

			try (Session session = driver.session()) {
				session.run("CREATE CONSTRAINT");
				Assertions.fail("An exception was expected");
			}
			catch (Neo4jException ex) {
				assertThat(HBD.constraintProbablyRequiredEnterpriseEdition(ex, connectionDetails)).isFalse();
			}
		}
	}

}
