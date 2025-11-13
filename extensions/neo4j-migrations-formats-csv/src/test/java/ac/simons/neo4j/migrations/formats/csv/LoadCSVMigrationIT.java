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
package ac.simons.neo4j.migrations.formats.csv;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadCSVMigrationIT {

	@SuppressWarnings("resource")
	protected final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("migrations.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	Driver driver;

	@BeforeAll
	void initDriver() {
		Config config = Config.builder().withLogging(Logging.none()).build();

		this.neo4j.start();
		this.driver = GraphDatabase.driver(this.neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", this.neo4j.getAdminPassword()), config);
	}

	@BeforeEach
	void clearDatabase() {
		new Migrations(MigrationsConfig.defaultConfig(), this.driver).clean(true);
		try (Session session = this.driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
		}
	}

	@AfterAll
	void closeDriver() {

		this.driver.close();
	}

	@Test
	void loadCSVShouldWork() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.formats.csv")
			.withLocationsToScan("classpath:books")
			.build(), this.driver);
		migrations.apply();

		try (var session = this.driver.session()) {
			var author = session
				.run("MATCH (a)-[r:WROTE]->(b:Book {title: 'Designing Data-Intensive Applications'}) RETURN a")
				.single()
				.get("a")
				.get("name")
				.asString();
			assertThat(author).isEqualTo("Martin Kleppmann");
		}
	}

}
