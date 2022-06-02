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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogBasedMigrationIT {

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "LATEST", "UNDEFINED" }, mode = EnumSource.Mode.EXCLUDE)
	void databaseCatalogsShouldWork(Neo4jVersion version) throws IOException {

		DefaultCatalog expectedCatalog = new DefaultCatalog();
		expectedCatalog.addAll(MigrationVersion.withValue("1"), () -> Arrays.asList(
			Constraint.forNode("Person").named("c1").key("firstname", "surname"),
			Constraint.forNode("Book").named("c2").exists("isbn"),
			Constraint.forRelationship("LIKED").named("c3").exists("day"),
			Constraint.forNode("Book").named("c4").unique("isbn"),
			Constraint.forRelationship("LIKED").named("c5").exists("x,liked.y"),
			Constraint.forNode("Person").named("c6")
				.key("firstname", "surname", "person.whatever", "person.a,person.b 😱")
		), false);

		// Unclosed on purpose, otherwise reuse is without use.
		Neo4jContainer<?> neo4j = getNeo4j(version, true);
		Config config = Config.builder().withLogging(Logging.none()).build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
			AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
			Session session = driver.session()) {
			Catalog catalog;
			if (version == Neo4jVersion.V4_2) {
				catalog = session.writeTransaction(tx -> DatabaseCatalog.of(version, tx));
			} else {
				catalog = session.readTransaction(tx -> DatabaseCatalog.of(version, tx));
			}
			assertThat(catalog.getItems()).isNotEmpty();
			assertThat(catalog.getItems()).allMatch(expectedCatalog::containsEquivalentItem);
		} finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "LATEST", "UNDEFINED" }, mode = EnumSource.Mode.EXCLUDE)
	void verificationShouldFailHard(Neo4jVersion version) throws IOException {

		Neo4jContainer<?> neo4j = getNeo4j(version, true);
		Config config = Config.builder().withLogging(Logging.none()).build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
			AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			Migrations migrations;
			migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:xml/actual-migrations").build(), driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
				.withMessage(
					"Could not apply migration 10 (\"Assert empty database\") verification failed: Catalogs are neither identical nor equivalent.");
		} finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "LATEST", "UNDEFINED" }, mode = EnumSource.Mode.EXCLUDE)
	void catalogBasedMigrationShouldWork(Neo4jVersion version) throws IOException {

		Neo4jContainer<?> neo4j = getNeo4j(version, false);
		Config config = Config.builder().withLogging(Logging.none()).build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
			AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			Migrations migrations;
			migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:xml/actual-migrations").build(), driver);
			if (version == Neo4jVersion.V3_5) {
				// We don't have constraint names, so the last verification will fail as it doesn't allow equivalency
				assertThatExceptionOfType(MigrationsException.class)
					.isThrownBy(migrations::apply)
					.withMessage("Could not apply migration 60 (\"Assert current catalog\") verification failed: Database schema and the catalog are equivalent but the verification requires them to be identical.");
			} else {
				Optional<MigrationVersion> optionalMigrationVersion = migrations.apply();
				assertThat(optionalMigrationVersion)
					.hasValue(MigrationVersion.withValue("70"));
				try (Session session = driver.session()) { // The last migration should clean everything not from Neo4j-Migrations itself
					assertThat(session.run(version.getShowConstraints()).list())
						.map(r -> r.get("name").asString())
						.allMatch(s -> s.contains("__Neo4jMigration"));

					assertThat(session.run(version.getShowIndexes()).list())
						.map(r -> r.get("name").asString())
						.allMatch(s -> s.contains("__Neo4jMigration"));
				}
			}
		} finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	private Neo4jContainer<?> getNeo4j(Neo4jVersion version, boolean createDefaultConstraints) throws IOException {
		Neo4jContainer<?> neo4j = new Neo4jContainer<>(String.format("neo4j:%s-enterprise", version.toString()))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.withReuse(version.hasIdempotentOperations() && TestcontainersConfiguration.getInstance()
				.environmentSupportsReuse())
			.withLabel("ac.simons.neo4j.migrations.core", this.getClass().getSimpleName() + "-" + version.name());
		neo4j.start();

		// We might reuse the containers from which we can easily drop the constraints again
		// without using our own mechanism which would defeat the purpose of testing it
		if (version.hasIdempotentOperations()) {
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), ConstraintsIT.NO_DRIVER_LOGGING_CONFIG);
				Session session = driver.session()) {

				List<String> constraintNames = session.run(version.getShowConstraints()).list(r -> r.get("name").asString());
				constraintNames.forEach(name -> session.run("DROP CONSTRAINT " + name).consume());
				List<String> indexNames = session.run(version.getShowIndexes()).list(r -> r.get("name").isNull() ? r.get("indexName").asString() : r.get("name").asString());
				indexNames.forEach(name -> session.run("DROP INDEX " + name).consume()); // this will also drop the lookup indexes ;(
				session.run("MATCH (n) DETACH DELETE n");
			}
		}

		if (!createDefaultConstraints) {
			return neo4j;
		}

		String cypherResource = String.format("/constraints/Create_%s_test_constraints.cypher", version.name());
		try (
			BufferedReader cypherReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
				getClass().getResourceAsStream(cypherResource))));
			Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), ConstraintsIT.NO_DRIVER_LOGGING_CONFIG);
			Session session = driver.session()) {
			String cmd;
			while ((cmd = cypherReader.readLine()) != null) {
				if (cmd.trim().isEmpty()) {
					continue;
				}
				assertThat(session.run(cmd).consume().counters().constraintsAdded()).isNotZero();
			}
			// Add our own constraints
			Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
			for (Constraint constraint : MigrationsLock.REQUIRED_CONSTRAINTS) {
				ResultSummary summary = session.run(renderer.render(constraint,
					RenderConfig.create().forVersionAndEdition(version, Neo4jEdition.ENTERPRISE))).consume();
				assertThat(summary.counters().constraintsAdded()).isOne();
			}
		}

		return neo4j;
	}
}
