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
package ac.simons.neo4j.migrations.maven;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
public class InfoMojoIT {

	@SuppressWarnings("resource")
	protected final Neo4jContainer<?> neo4j = new Neo4jContainer<>(System.getProperty("migrations.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	@Test
	public void shouldLog() throws Exception {

		neo4j.start();
		try (var driver = GraphDatabase.driver(neo4j.getBoltUrl(),
			AuthTokens.basic("neo4j", neo4j.getAdminPassword()))) {

			try (Session session = driver.session()) {
				session.run("MATCH (n) DETACH DELETE n").consume();
			}

			Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
			InfoMojo cmd = new InfoMojo();

			String result = tapSystemErr(() -> {
				cmd.withMigrations(migrations);
				System.out.flush();
			});
			assertThat(result)
				.contains("Neo4j/4")
				.contains("No migrations found");
		}
	}
}
