/*
 * Copyright 2020-2026 the original author or authors.
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

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Making sure that migrations boots up fine with a Cypher 25 default database and can
 * also derive a catalog from it.
 *
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ParameterizedClass
@ValueSource(strings = { "CYPHER 5", "CYPHER 25" })
class CatalogDiscoveryCypherVersionsIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = new Neo4jContainer("neo4j:2025.11.2-enterprise")
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withPlugins("apoc")
		.withReuse(true);

	private final MigrationsConfig config = MigrationsConfig.builder()
		.withLocationsToScan("classpath:cypher25catalog")
		.build();

	Driver driver;

	@Parameter
	String cypherVersion;

	@BeforeAll
	void initDriver() {
		Config config = Config.builder().build();

		this.neo4j.start();
		this.driver = GraphDatabase.driver(this.neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", this.neo4j.getAdminPassword()), config);
	}

	@BeforeEach // Need to be BeforeEach because parameter is injected late
	void configureCypherVersion() {
		try (var session = this.driver.session()) {
			// Why not parameterizable?! :'(
			session.run("ALTER DATABASE neo4j SET DEFAULT LANGUAGE " + this.cypherVersion);
		}
	}

	@AfterEach
	void dropConstraints() {
		try (var session = this.driver.session()) {
			session.run("DROP CONSTRAINT constraint_name1 IF EXISTS");
		}
	}

	@Test
	void shouldInitializeDatabase() {

		var migrations = new Migrations(this.config, this.driver);
		migrations.clean(true);

		var info = migrations.info();
		assertThat(info.getLastAppliedVersion()).isEmpty();

		migrations.apply();
		info = migrations.info();
		assertThat(info.getLastAppliedVersion()).map(MigrationVersion::getValue).hasValue("0001");
	}

	@Test
	void shouldBeAbleToIgnoreMyOwnConstraints() {

		var migrations = new Migrations(this.config, this.driver);
		migrations.clean(true);
		var info = migrations.info();
		assertThat(info.getLastAppliedVersion()).isEmpty();

		var catalog = migrations.getDatabaseCatalog();
		// Internal constraints are filtered, so this one is empty
		assertThat(catalog.isEmpty()).isTrue();
	}

	@Test
	void shouldBeAbleToReadAllUniquenessConstraints() {

		var migrations = new Migrations(this.config, this.driver);
		migrations.clean(true);
		migrations.apply();
		var info = migrations.info();
		assertThat(info.getLastAppliedVersion()).map(MigrationVersion::getValue).hasValue("0001");

		var catalog = migrations.getDatabaseCatalog();

		assertThat(catalog.getItems().stream().map(CatalogItem::getName))
			.containsExactlyInAnyOrder(Name.of("constraint_name1"));

	}

}
