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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SkipArm64IncompatibleConfiguration.class)
class CatalogBasedMigrationIT {

	static Stream<Arguments> shouldApplyResources() {
		return Stream.of(Arguments.of(new SkipArm64IncompatibleConfiguration.VersionUnderTest(Neo4jVersion.V4_4, true)),
				Arguments.of(new SkipArm64IncompatibleConfiguration.VersionUnderTest(Neo4jVersion.V5, true)));
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void databaseCatalogsShouldWork(SkipArm64IncompatibleConfiguration.VersionUnderTest version) throws IOException {

		DefaultCatalog expectedCatalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		expectedCatalog.addAll(MigrationVersion.withValue("1"),
				() -> Arrays.asList(Constraint.forNode("Person").named("c1").key("firstname", "surname"),
						Constraint.forNode("Book").named("c2").exists("isbn"),
						Constraint.forRelationship("LIKED").named("c3").exists("day"),
						Constraint.forNode("Book").named("c4").unique("isbn"),
						Constraint.forRelationship("LIKED").named("c5").exists("x,liked.y"),
						Constraint.forNode("Person")
							.named("c6")
							.key("firstname", "surname", "person.whatever", "person.a,person.b 😱")),
				false);

		// Unclosed on purpose, otherwise reuse is without use.
		Neo4jContainer neo4j = getNeo4j(version.value, version.enterprise, true);
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config); Session session = driver.session()) {
			Catalog catalog;
			if (version.value == Neo4jVersion.V4_2) {
				catalog = session.executeWrite(tx -> DatabaseCatalog.of(version.value, tx, true));
			}
			else {
				catalog = session.executeRead(tx -> DatabaseCatalog.of(version.value, tx, true));
			}
			assertThat(catalog.getItems()).isNotEmpty();
			assertThat(catalog.getItems()).allMatch(expectedCatalog::containsEquivalentItem);
		}
		finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void verificationShouldFailHard(SkipArm64IncompatibleConfiguration.VersionUnderTest version) throws IOException {

		Neo4jContainer neo4j = getNeo4j(version.value, version.enterprise, true);
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			Migrations migrations;
			migrations = new Migrations(
					MigrationsConfig.builder().withLocationsToScan("classpath:catalogbased/actual-migrations").build(),
					driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
				.withMessage(
						"Could not apply migration 10 (\"Assert empty database\") verification failed: Catalogs are neither identical nor equivalent.");
		}
		finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	@ParameterizedTest // GH-573
	@MethodSource
	void shouldApplyResources(SkipArm64IncompatibleConfiguration.VersionUnderTest version) throws IOException {

		Neo4jContainer neo4j = getNeo4j(version.value, version.enterprise, false);
		Config config = Config.builder().build();

		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {
			Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);

			int appliedMigrations = migrations.apply(
					Objects
						.requireNonNull(MigrationsIT.class.getResource("/manual_resources/V000__Create_graph.cypher")),
					Objects
						.requireNonNull(MigrationsIT.class.getResource("/manual_resources/V000__Refactor_graph.xml")));

			assertThat(appliedMigrations).isEqualTo(2);

			try (Session session = driver.session()) {
				long cnt = session.run(
						"MATCH (m:Person {name:'Michael'}) -[:MAG]-> (n:Person {name:'Tina', klug: true}) RETURN count(m)")
					.single()
					.get(0)
					.asLong();
				assertThat(cnt).isOne();
			}
		}
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void catalogBasedMigrationShouldWork(SkipArm64IncompatibleConfiguration.VersionUnderTest version)
			throws IOException {

		Neo4jContainer neo4j = getNeo4j(version.value, version.enterprise, false);
		Config config = Config.builder().build();

		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			try (Session session = driver.session()) {
				session.run("MATCH (n) DETACH DELETE n").consume();
			}

			Migrations migrations;
			migrations = new Migrations(
					MigrationsConfig.builder().withLocationsToScan("classpath:catalogbased/actual-migrations").build(),
					driver);
			if (version.value == Neo4jVersion.V3_5) {
				// We don't have constraint names, so the last verification will fail as
				// it doesn't allow equivalency
				assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
					.withMessage(
							"Could not apply migration 60 (\"Assert current catalog\") verification failed: Database schema and the catalog are equivalent but the verification requires them to be identical.");
			}
			else {
				Optional<MigrationVersion> optionalMigrationVersion = migrations.apply();
				assertThat(optionalMigrationVersion).hasValue(MigrationVersion.withValue("70"));
				try (Session session = driver.session()) { // The last migration should
															// clean everything not from
															// Neo4j-Migrations itself
					assertThat(session.run(version.value.getShowConstraints()).list())
						.map(r -> r.get("name").asString())
						.allMatch(s -> s.contains("__Neo4jMigration"));

					assertThat(session.run(version.value.getShowIndexes()).list())
						.filteredOn(r -> !"LOOKUP".equals(r.get("type", "UNKNOWN")))
						.map(r -> r.get("name").asString())
						.allMatch(s -> s.contains("__Neo4jMigration"));
				}
			}
			try (Session session = driver.session()) { // Data should not be cleansed
				assertThat(session.run("MATCH (b:Book)<-[:READ]-(p:Person) RETURN b.title").single().get(0).asString())
					.isEqualTo("Doctor Sleep");
				MigrationChain migrationChain = migrations.info();
				if (migrationChain.isApplied("45")) {
					List<Node> books = session.run("MATCH (b:Book) RETURN b ORDER BY b.name ASC")
						.list(r -> r.get("b").asNode());
					assertThat(books).hasSize(2).satisfies(n -> {
						assertThat(n.get("title").asString()).isEqualTo("Doctor Sleep");
						assertThat(n.get("gelesen").asBoolean()).isTrue();
					}, Index.atIndex(0)).satisfies(n -> {
						assertThat(n.get("title").asString()).isEqualTo("Fairy Tale");
						assertThat(n.get("gelesen").asBoolean()).isFalse();
					}, Index.atIndex(1));
				}
			}
		}
		finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	@Test
	void optionsCanBeIncluded() throws IOException {

		Neo4jContainer neo4j = getNeo4j(Neo4jVersion.V4_4, true, false);
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {
			try (Session session = driver.session()) {
				session.run("MATCH (n) DETACH DELETE n").consume();
			}
			Migrations migrations = new Migrations(MigrationsConfig.builder()
				.withLocationsToScan("classpath:catalogbased/actual-migrations-with-complete-verification")
				.build(), driver);
			Optional<MigrationVersion> optionalMigrationVersion = migrations.apply();
			assertThat(optionalMigrationVersion).hasValue(MigrationVersion.withValue("60"));
		}
		finally {
			if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
				neo4j.stop();
			}
		}
	}

	private Neo4jContainer getNeo4j(Neo4jVersion version, boolean enterprise, boolean createDefaultConstraints)
			throws IOException {
		return getNeo4j(version, null, enterprise, createDefaultConstraints);
	}

	private Neo4jContainer getNeo4j(Neo4jVersion version, String versionValue, boolean enterprise,
			boolean createDefaultConstraints) throws IOException {
		String theVersion = (versionValue != null) ? versionValue : version.toString();
		Neo4jContainer neo4j = new Neo4jContainer(
				String.format("neo4j:%s" + (enterprise ? "-enterprise" : ""), theVersion))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.withReuse(version.hasIdempotentOperations());
		neo4j.start();

		// We might reuse the containers from which we can easily drop the constraints
		// again
		// without using our own mechanism which would defeat the purpose of testing it
		if (version.hasIdempotentOperations()) {
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
					AuthTokens.basic("neo4j", neo4j.getAdminPassword()), ConstraintsIT.NO_DRIVER_LOGGING_CONFIG);
					Session session = driver.session()) {

				List<String> constraintNames = session.run(version.getShowConstraints())
					.list(r -> r.get("name").asString());
				constraintNames.forEach(name -> session.run("DROP CONSTRAINT " + name).consume());
				Map<String, String> indexNames = new HashMap<>();
				session.run(version.getShowIndexes())
					.forEachRemaining(r -> indexNames.put(
							r.get("name").isNull() ? r.get("indexName").asString() : r.get("name").asString(),
							r.get("type", "UNKNOWN")));

				indexNames.forEach((name, type) -> {
					if ("LOOKUP".equals(type)) {
						return;
					}
					session.run("DROP INDEX " + name).consume();
				});
				session.run("MATCH (n) DETACH DELETE n");
			}
		}

		if (!createDefaultConstraints) {
			return neo4j;
		}

		String cypherResource = String.format("/constraints/Create_%s_test_constraints.cypher", version.name());
		try (BufferedReader cypherReader = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(cypherResource))));
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
				ResultSummary summary = session
					.run(renderer.render(constraint,
							RenderConfig.create().forVersionAndEdition(version, Neo4jEdition.ENTERPRISE)))
					.consume();
				assertThat(summary.counters().constraintsAdded()).isOne();
			}
		}

		return neo4j;
	}

}
