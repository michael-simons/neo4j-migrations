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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.SummaryCounters;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Those tests here are unrelated to anything catalog based. They existed before and
 * ensure that the constraints needed by neo4j-migrations work as expected.
 *
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SkipArm64IncompatibleConfiguration.class)
class ConstraintsIT {

	public static final Config NO_DRIVER_LOGGING_CONFIG = Config.builder().build();

	@ParameterizedTest
	@CsvSource(nullValues = "N/A", textBlock = """
			Neo4j/3.5, false
			Neo4j/4.0, true
			neo4J/4.0, true
			Neo4j/4, true
			Neo4j/4.4, true
			4.2, true
			N/A, false""")
	void shouldDetectCorrectVersion(String v, boolean expected) {

		ConnectionDetails cd = new DefaultConnectionDetails(null, v, null, null, null, null);
		assertThat(HBD.is4xSeries(cd)).isEqualTo(expected);

		for (String edition : new String[] { "Community", "Enterprise" }) {
			cd = new DefaultConnectionDetails(null, v, edition, null, null, null);
			assertThat(HBD.is4xSeries(cd)).isEqualTo(expected);
		}
	}

	@ParameterizedTest
	@CsvSource(nullValues = "N/A", textBlock = """
			Neo4j/3.5, false
			Neo4j/4.0, false
			neo4J/4.0, false
			Neo4j/4, false
			Neo4j/4.4, true
			4.5, true
			5.0, true
			5, true
			4.2, false
			4.4.2, true
			N/A, false""")
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
			List<Record> constraints = session.run("call db.constraints()").list(Function.identity());
			for (Record constraint : constraints) {
				if (constraint.containsKey("name")) {
					try {
						session.run("DROP constraint " + constraint.get("name").asString());
						continue;
					}
					catch (Exception ignored) {
					}
				}
				session.run("DROP " + constraint.get("description").asString());
			}
		}
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldCreateConstraints(SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		final String s0 = "CREATE CONSTRAINT $name ON ( book:Book ) ASSERT book.isbn IS UNIQUE";
		final String s1 = "CREATE CONSTRAINT ON ( person:Person ) ASSERT person.name IS UNIQUE";
		final String s2 = "CREATE CONSTRAINT ON ( movie:Movie ) ASSERT movie.title IS UNIQUE";

		Neo4jContainer neo4j = getNeo4j(version.asTag());
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
			MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

			Supplier<String> errorMessage = () -> "oops";
			ConnectionDetails cd = ctx.getConnectionDetails();
			try (Session session = ctx.getSession()) {
				assertThat(HBD.silentCreateConstraintOrIndex(cd, session, s0, "AAA", errorMessage)).isOne();
				assertThat(HBD.silentCreateConstraintOrIndex(cd, session, s1, "BBB", errorMessage)).isOne();
				assertThat(HBD.silentCreateConstraintOrIndex(cd, session, s2, null, errorMessage)).isOne();
			}

			boolean is35 = Neo4jVersion.V3_5 == version.value;
			String[] descriptions = Stream.of(s0, s1, s2).map(s -> {
				String result = s.replace("CREATE ", "").replace("$name ", "");
				return is35 ? result : result.replaceAll("ASSERT (.+\\..+) IS", "ASSERT ($1) IS");
			}).toArray(String[]::new);
			try (Session session = ctx.getSession()) {
				List<Record> result = session.run("call db.constraints()").list(Function.identity());

				assertThat(result.stream().map(r -> r.get("description").asString()))
					.containsExactlyInAnyOrder(descriptions);

				Stream<String> names = result.stream().map(r -> r.get("name").asString(null));
				if (is35) {
					assertThat(names).allMatch(Objects::isNull);
				}
				else {
					Consumer<String> isRandomConstraintName = s -> assertThat(s).startsWith("constraint_");
					assertThat(names).hasSize(3)
						.satisfies(s -> assertThat(s).isEqualTo("AAA"), Index.atIndex(0))
						.satisfies(isRandomConstraintName, Index.atIndex(1));
				}
			}
		}
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldDropConstraints(SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		final String s0 = "CREATE CONSTRAINT $name ON ( book:Book ) ASSERT book.isbn IS UNIQUE";

		Neo4jContainer neo4j = getNeo4j(version.asTag());
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
			MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

			Supplier<String> errorMessage = () -> "oops";
			ConnectionDetails cd = ctx.getConnectionDetails();

			try (Session session = ctx.getSession()) {
				assertThat(HBD.silentCreateConstraintOrIndex(cd, session, s0, "AAA", errorMessage)).isOne();
			}

			int dropped;
			try (Session session = ctx.getSession()) {
				if (Neo4jVersion.V3_5 == version.value) {
					dropped = HBD.silentDropConstraint(cd, session,
							"DROP CONSTRAINT ON ( n:Book ) ASSERT n.isbn IS UNIQUE", "AAA");
				}
				else {
					dropped = HBD.silentDropConstraint(cd, session, "messed up statement on purpose", "AAA");
				}
			}
			assertThat(dropped).isOne();
		}
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldThrowExceptionWhenConstraintsWithSameNameExists(
			SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		Neo4jContainer neo4j = getNeo4j(version.asTag());
		Config config = Config.builder().build();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

			MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
			MigrationContext ctx = new DefaultMigrationContext(migrationsConfig, driver);

			Supplier<String> errorMessage = () -> "oops";
			ConnectionDetails cd = ctx.getConnectionDetails();
			try (Session session = ctx.getSession()) {

				int created = session.run("CREATE CONSTRAINT X ON (book:Book) ASSERT book.isbn IS UNIQUE")
					.consume()
					.counters()
					.constraintsAdded();
				assertThat(created).isOne();

				assertThatExceptionOfType(MigrationsException.class)
					.isThrownBy(() -> HBD.silentCreateConstraintOrIndex(cd, session,
							"CREATE CONSTRAINT X ON (n:SomethingElse) ASSERT n.whatever IS UNIQUE", null, errorMessage))
					.matches(HBD::constraintWithNameAlreadyExists);
			}
		}
	}

	SummaryCounters executeAndConsume(Session session, String statement) {
		return session.run(statement).consume().counters();
	}

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldPreventDuplicateVersionsWithTarget(SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		Neo4jContainer neo4j = getNeo4j(version.asTag());
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), NO_DRIVER_LOGGING_CONFIG)) {

			MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();

			Migrations migrations = new Migrations(migrationsConfig, driver);
			migrations.clean(true);
			migrations.apply();

			try (Session session = new DefaultMigrationContext(migrationsConfig, driver).getSchemaSession()) {

				session.run("CREATE (:__Neo4jMigration {version: '1', migrationTarget: 'x'})");
				if (Neo4jVersion.V4_3 == version.value) {
					assertThat(executeAndConsume(session,
							"CREATE (:__Neo4jMigration {version: '1', migrationTarget: 'x'})")
						.nodesCreated()).isOne();

					CleanResult result = migrations.clean(false);
					assertThat(result.getConstraintsRemoved()).isZero();

					result = migrations.clean(true);
					assertThat(result.getConstraintsRemoved()).isEqualTo(2L);
				}
				else {
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

	private Neo4jContainer getNeo4j(String tag) {

		Neo4jContainer neo4j = new Neo4jContainer(tag).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes").withReuse(true);
		neo4j.start();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), NO_DRIVER_LOGGING_CONFIG)) {
			dropAllConstraints(driver);
		}
		return neo4j;
	}

}
