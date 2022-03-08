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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.SummaryCounters;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * @author Michael J. Simons
 * @soundtrack Guns n' Roses - Appetite For Democracy 3D
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConstraintsIT {

	@ParameterizedTest
	@CsvSource(value = { "Neo4j/3.5, false", "Neo4j/4.0, true", "neo4J/4.0, true", "Neo4j/4, true", "Neo4j/4.4, true",
		"4.2, true", "N/A, false" }, nullValues = "N/A")
	void shouldDetectCorrectVersion(String v, boolean expected) {

		ConnectionDetails cd = new DefaultConnectionDetails(null, v, null, null, null, null);
		assertThat(HBD.is4xSeries(cd)).isEqualTo(expected);

		for (String edition : new String[] { "Community", "Enterprise" }) {
			cd = new DefaultConnectionDetails(null, v, edition, null, null, null);
			assertThat(HBD.is4xSeries(cd)).isEqualTo(expected);
		}
	}

	@ParameterizedTest
	@CsvSource(value = { "Neo4j/3.5, false", "Neo4j/4.0, false", "neo4J/4.0, false", "Neo4j/4, false",
		"Neo4j/4.4, true",
		"4.5, true", "5.0, true", "5, true", "4.2, false", "4.4.2, true", "N/A, false" }, nullValues = "N/A")
	void shouldDetect44OrHigher(String v, boolean expected) {

		ConnectionDetails cd = new DefaultConnectionDetails(null, v, null, null, null, null);
		assertThat(HBD.is44OrHigher(cd)).isEqualTo(expected);

		for (String edition : new String[] { "Community", "Enterprise" }) {
			cd = new DefaultConnectionDetails(null, v, edition, null, null, null);
			assertThat(HBD.is44OrHigher(cd)).isEqualTo(expected);
		}
	}

	void dropAllConstraints(Driver driver) {

		try (Session session = driver.session()) {
			session.run("call db.constraints()")
				.list(record -> "DROP " + record.get("description").asString())
				.forEach(session::run);
		}
	}

	static boolean is35(String tag) {
		return "neo4j:3.5".equals(tag);
	}

	@ParameterizedTest
	@ValueSource(strings = { "neo4j:3.5", "neo4j:4.0", "neo4j:4.4" })
	void shouldCreateConstraints(String tag) {

		final String s0 = "CREATE CONSTRAINT $name ON ( book:Book ) ASSERT book.isbn IS UNIQUE";
		final String s1 = "CREATE CONSTRAINT ON ( person:Person ) ASSERT person.name IS UNIQUE";
		final String s2 = "CREATE CONSTRAINT ON ( movie:Movie ) ASSERT movie.title IS UNIQUE";

		try (Neo4jContainer<?> neo4j = getNeo4j(tag)) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
				MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

				Supplier<String> errorMessage = () -> "oops";
				ConnectionDetails cd = ctx.getConnectionDetails();
				try (Session session = ctx.getSession()) {
					assertThat(HBD.silentCreateConstraint(cd, session, s0, "AAA", errorMessage)).isOne();
					assertThat(HBD.silentCreateConstraint(cd, session, s1, "BBB", errorMessage)).isOne();
					assertThat(HBD.silentCreateConstraint(cd, session, s2, null, errorMessage)).isOne();
				}

				boolean is35 = is35(tag);
				String[] descriptions = Stream.of(s0, s1, s2).map(s -> {
						String result = s
							.replace("CREATE ", "")
							.replace("$name ", "");
						return is35 ? result : result.replaceAll("ASSERT (.+\\..+) IS", "ASSERT ($1) IS");
					}
				).toArray(String[]::new);
				try (Session session = ctx.getSession()) {
					List<Record> result = session.run("call db.constraints()").list(Function.identity());

					assertThat(result.stream().map(r -> r.get("description").asString()))
						.containsExactlyInAnyOrder(descriptions);

					Stream<String> names = result.stream().map(r -> r.get("name").asString(null));
					if (is35) {
						assertThat(names).allMatch(Objects::isNull);
					} else {
						Consumer<String> isRandomConstraintName = s -> assertThat(s).startsWith("constraint_");
						assertThat(names)
							.hasSize(3)
							.satisfies(s -> assertThat(s).isEqualTo("AAA"), Index.atIndex(0))
							.satisfies(isRandomConstraintName, Index.atIndex(1));
					}
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "neo4j:3.5", "neo4j:4.0", "neo4j:4.4" })
	void shouldDropConstraints(String tag) {

		final String s0 = "CREATE CONSTRAINT $name ON ( book:Book ) ASSERT book.isbn IS UNIQUE";

		try (Neo4jContainer<?> neo4j = getNeo4j(tag)) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
				MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

				Supplier<String> errorMessage = () -> "oops";
				ConnectionDetails cd = ctx.getConnectionDetails();

				try (Session session = ctx.getSession()) {
					assertThat(HBD.silentCreateConstraint(cd, session, s0, "AAA", errorMessage)).isOne();
				}

				int dropped;
				try (Session session = ctx.getSession()) {
					if (is35(tag)) {
						dropped = HBD.silentDropConstraint(cd, session,
							"DROP CONSTRAINT ON ( n:Book ) ASSERT n.isbn IS UNIQUE", "AAA");
					} else {
						dropped = HBD.silentDropConstraint(cd, session, "messed up statement on purpose", "AAA");
					}
				}
				assertThat(dropped).isOne();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "neo4j:4.3", "neo4j:4.4" })
	void shouldThrowExceptionWhenConstraintsWithSameNameExists(String tag) {

		try (Neo4jContainer<?> neo4j = getNeo4j(tag)) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
				MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

				Supplier<String> errorMessage = () -> "oops";
				ConnectionDetails cd = ctx.getConnectionDetails();
				try (Session session = ctx.getSession()) {

					int created = session.run("CREATE CONSTRAINT X ON (book:Book) ASSERT book.isbn IS UNIQUE").consume()
						.counters().constraintsAdded();
					assertThat(created).isOne();

					assertThatExceptionOfType(MigrationsException.class)
						.isThrownBy(() -> HBD.silentCreateConstraint(cd, session,
							"CREATE CONSTRAINT X ON (n:SomethingElse) ASSERT n.whatever IS UNIQUE", null, errorMessage))
						.matches(HBD::constraintWithNameAlreadyExists);
				}
			}
		}
	}

	SummaryCounters executeAndConsume(Session session, String statement) {
		return session.run(statement).consume().counters();
	}

	@ParameterizedTest
	@ValueSource(strings = { "neo4j:4.3", "neo4j:4.4", "neo4j:4.4-enterprise" })
	void shouldPreventDuplicateVersionsWithTarget(String tag) {

		try (Neo4jContainer<?> neo4j = getNeo4j(tag)) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();

				Migrations migrations = new Migrations(migrationsConfig, driver);
				migrations.clean(true);
				migrations.apply();

				try (Session session = new DefaultMigrationContext(migrationsConfig, driver).getSchemaSession()) {

					session.run("CREATE (:__Neo4jMigration {version: '1', migrationTarget: 'x'})");
					if ("neo4j:4.3".equals(tag)) {
						assertThat(executeAndConsume(session,
							"CREATE (:__Neo4jMigration {version: '1', migrationTarget: 'x'})").nodesCreated()).isOne();

						CleanResult result = migrations.clean(false);
						assertThat(result.getConstraintsRemoved()).isZero();

						result = migrations.clean(true);
						assertThat(result.getConstraintsRemoved()).isEqualTo(2L);
					} else {
						assertThatExceptionOfType(ClientException.class)
							.isThrownBy(() -> executeAndConsume(session,
								"CREATE (:__Neo4jMigration {version: '1', migrationTarget: 'x'})"))
							.withMessageEndingWith(
								"already exists with label `__Neo4jMigration` and properties `version` = '1', `migrationTarget` = 'x'");

						CleanResult result = migrations.clean(false);
						assertThat(result.getConstraintsRemoved()).isZero();

						result = migrations.clean(true);
						assertThat(result.getConstraintsRemoved()).isEqualTo(3L);
					}
				}
			}
		}
	}

	private Neo4jContainer<?> getNeo4j(String tag) {
		Neo4jContainer<?> neo4j = new Neo4jContainer<>(tag)
			.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.withLabel("ac.simons.neo4j.migrations.core", "HBDIT");
		neo4j.start();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
			AuthTokens.basic("neo4j", neo4j.getAdminPassword()))) {
			dropAllConstraints(driver);
		}
		return neo4j;
	}
}
