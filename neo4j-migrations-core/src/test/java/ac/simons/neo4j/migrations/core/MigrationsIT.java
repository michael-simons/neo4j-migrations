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

import ac.simons.neo4j.migrations.core.MigrationChain.ChainBuilderMode;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogDiff;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Normalize;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;
import ac.simons.neo4j.migrations.core.refactorings.Rename;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.testcontainers.containers.Neo4jContainer;

/**
 * @author Michael J. Simons
 */
class MigrationsIT extends TestBase {

	@Test
	void shouldApplyMigrations() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1").build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
			"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(5);

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements())
			.hasSizeGreaterThan(0)
			.allMatch(element -> element.getState() == MigrationState.APPLIED);
		assertThat(migrationChain.getLastAppliedVersion()).hasValue(MigrationVersion.withValue("023.1.1"));
	}

	@Test // GH-573
	void shouldIgnoreNullRefactorings() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		Counters counters = migrations.apply((Refactoring[]) null);
		assertThat(counters).isEqualTo(Counters.empty());
	}

	@Test // GH-573
	void shouldIgnoreEmptyRefactorings() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		Counters counters = migrations.apply(new Refactoring[0]);
		assertThat(counters).isEqualTo(Counters.empty());
	}

	@Test // GH-573
	void shouldApplyRefactorings() {

		try (Session session = driver.session()) {
			session.run("CREATE (m:Person {name:'Michael'}) -[:LIKES]-> (n:Person {name:'Tina', klug:'ja'})");
		}

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		Counters counters = migrations.apply(
			Rename.type("LIKES", "MAG"),
			Normalize.asBoolean("klug", Collections.singletonList("ja"), Collections.singletonList("nein"))
		);
		assertThat(counters.propertiesSet()).isOne();
		assertThat(counters.typesRemoved()).isOne();
		assertThat(counters.typesAdded()).isOne();

		try (Session session = driver.session()) {
			long cnt = session.run(
					"MATCH (m:Person {name:'Michael'}) -[:MAG]-> (n:Person {name:'Tina', klug: true}) RETURN count(m)")
				.single().get(0).asLong();
			assertThat(cnt).isOne();
		}
	}

	@Test
	void shouldBeAwareThatThingsHaveBeenApplied() {
		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan(
			"classpath:doublewillfail").build(), driver);
		assertThat(migrations.apply()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo("0001"));
		MigrationChain info = migrations.info();
		assertThat(migrations.apply()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo("0001"));
		MigrationChain info2 = migrations.info();
		assertThat(info.getElements()).first().satisfies(e -> assertThat(e.getInstalledOn()).isEqualTo(info2.getElements().iterator().next().getInstalledOn()));
	}

	@Test
	void shouldFailIfNoMigrationsAreDiscoveredButThingsAreInsideTheDatabase() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();
		assertThat(lengthOfMigrations(driver, null)).isEqualTo(5);

		migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::info)
			.withMessage("More migrations have been applied to the database than locally resolved.");
	}

	@Test
	void localOnlyInfoShouldWork() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(5);

		MigrationChain migrationChain = migrations.info(ChainBuilderMode.LOCAL);
		assertThat(migrationChain.getElements())
			.isNotEmpty()
			.allMatch(element -> element.getState() == MigrationState.PENDING);
	}

	@Test
	void remoteOnlyInfoShouldWork() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
				"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();
		assertThat(lengthOfMigrations(driver, null)).isEqualTo(5);

		migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
		MigrationChain migrationChain = migrations.info(ChainBuilderMode.REMOTE);
		assertThat(migrationChain.getElements())
			.isNotEmpty()
			.allMatch(element -> element.getState() == MigrationState.APPLIED);
	}

	@Test
	void shouldRecordExecutionTime() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset4").build(), driver);
		migrations.apply();

		try (Session session = driver.session()) {
			long executionTime = session.run(
				"MATCH (:__Neo4jMigration {version: 'BASELINE'}) -[r:MIGRATED_TO]->() RETURN r.in AS executionTime")
				.single().get("executionTime").asIsoDuration().nanoseconds();
			assertThat(executionTime).isGreaterThanOrEqualTo(500000000L);
		}
	}

	@Test
	void shouldNotTryToImpersonateWithEmptyName() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
				"ac.simons.neo4j.migrations.core.test_migrations.changeset4")
			.withImpersonatedUser("  	 ")
			.build(), driver);
		assertThat(migrations.info()).isNotNull();
	}

	@Test
	void shouldFailWithNewMigrationsInBetween() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset3").build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(1);

		Migrations failingMigrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.build(), driver);

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
			.withMessage("Unexpected migration at index 0: 001 (\"FirstMigration\").");
	}

	@Test
	void shouldNotSwallowMigrationExceptions() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("classpath:broken").build(), driver);
		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(migrations::apply)
			.withCauseInstanceOf(ClientException.class);
	}

	@Test
	void shouldFailWithChangedMigrations() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			Migrations failingMigrations = new Migrations(configuration, driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
				.withMessage("Checksum of 2 (\"Some\") changed!");
		} finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test // GH-237
	void changedMigrationsShouldBeAllowedWhenValidateIsOff() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).withValidateOnMigrate(false).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(),
				Collections.singletonList("CREATE INDEX node_index_name FOR (n:Person) ON (n.surname)"));

			migrations = new Migrations(configuration, driver);
			migrations.apply();

			try (Session session = driver.session()) {
				String version = session.run(
					"MATCH (:__Neo4jMigration {version: '2'}) -[r:MIGRATED_TO]->(t) RETURN t.version AS version")
					.single().get("version").asString();
				assertThat(version).isEqualTo("3");
			}
		} finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test
	void shouldVerifyChecksums() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(), Collections.singletonList("MATCH (n) RETURN n"));

			migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(3);
		} finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test // GH-238
	void shouldNotFailWithDifferentLineEndings() {

		MigrationsConfig configuration = MigrationsConfig.builder()
			.withLocationsToScan("classpath:ml/dos")
			.withAutocrlf(true)
			.build();
		Migrations migrations = new Migrations(configuration, driver);

		Optional<MigrationVersion> finalVersion = migrations.apply();
		assertThat(lengthOfMigrations(driver, null)).isEqualTo(1);
		assertThat(finalVersion).flatMap(MigrationVersion::getOptionalDescription).hasValue("Just a couple of matches");

		configuration = MigrationsConfig.builder()
			.withLocationsToScan("classpath:ml/unix")
			.withAutocrlf(true)
			.build();
		migrations = new Migrations(configuration, driver);

		finalVersion = migrations.apply();
		assertThat(lengthOfMigrations(driver, null)).isEqualTo(1);
		assertThat(finalVersion).flatMap(MigrationVersion::getOptionalDescription).hasValue("Just a couple of matches");
	}

	@Test
	void shouldNotFailOnDisabledAuth() {

		try (Neo4jContainer<?> containerWithoutAuth = new Neo4jContainer<>(TestBase.DEFAULT_NEO4J_IMAGE).withoutAuthentication()) {
			containerWithoutAuth.start();

			try (Driver driverWithoutAuth = GraphDatabase.driver(containerWithoutAuth.getBoltUrl())) {

				MigrationsConfig config = MigrationsConfig.builder()
						.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1").build();
				Migrations migrations = new Migrations(config, driverWithoutAuth);

				assertThat(migrations.apply()).map(MigrationVersion::getValue).hasValue("002");

				MigrationChain info = migrations.info();
				assertThat(info.getUsername()).isEqualTo("anonymous");
				assertThat(info.getElements()).element(1)
						.satisfies(e -> assertThat(e.getInstalledBy()).hasValue(System.getProperty("user.name") + "/anonymous"));
			}
		}
	}

	@Test // GH-647
	void shouldFailAsGracefullyAsItGetsWhenEditionMismatch() {

		MigrationsConfig configuration = MigrationsConfig.builder()
			.withLocationsToScan("classpath:ee")
			.withAutocrlf(true)
			.build();
		Migrations migrations = new Migrations(configuration, driver);
		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(migrations::apply)
			.withMessage("Migration `01 (\"Use some enterprise features\")` uses a constraint that requires "
				+ "Neo4j Enterprise Edition but the database connected to is a Community edition, "
				+ "you might want to add a guard like `// assume that edition is enterprise` in your script"
			);
	}

	private static List<File> createMigrationFiles(int n, File dir) throws IOException {
		List<File> files = new ArrayList<>();
		for (int i = 1; i <= n; ++i) {
			File aFile = new File(dir, "V" + i + "__Some.cypher");
			aFile.createNewFile();
			files.add(aFile);
			Files.write(aFile.toPath(), Collections.singletonList("MATCH (n) RETURN n"));
		}
		return files;
	}

	@ParameterizedTest
	@EnumSource(MigrationsConfig.TransactionMode.class)
	void shouldApplyResourceBasedMigrations(MigrationsConfig.TransactionMode transactionMode) {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withTransactionMode(transactionMode)
			.withLocationsToScan(
			"classpath:my/awesome/migrations", "classpath:some/changeset").build(), driver);

		Catalog localCatalog = migrations.getLocalCatalog();
		assertThat(localCatalog.getItems()).hasSize(2);
		Catalog databaseCatalog = migrations.getDatabaseCatalog();
		assertThat(databaseCatalog.getItems()).isEmpty();
		CatalogDiff diff = CatalogDiff.between(databaseCatalog, localCatalog);
		assertThat(diff.getItemsOnlyInRight()).containsAll(localCatalog.getItems());

		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(12);

		databaseCatalog = migrations.getDatabaseCatalog();
		assertThat(databaseCatalog.getItems()).hasSize(1);
		diff = CatalogDiff.between(databaseCatalog, localCatalog);
		assertThat(diff.getItemsOnlyInRight()).map(CatalogItem::getName).containsExactly(Name.of("constraint_with_options"));

		try (Session session = driver.session()) {
			String prop = session.run("MATCH (s:Stuff) RETURN s.prop").single().get(0).asString();
			String value = "\n"
				+ "this is a nice string with\n"
				+ "// a comment\n"
				+ "  // in it!\n";
			assertThat(prop).isEqualTo(value);

			List<String> checksums = session.run("MATCH (m:__Neo4jMigration) RETURN m.checksum AS checksum ORDER BY CASE WHEN m.version = 'BASELINE' THEN '0000' ELSE m.version END ASC")
				.list(r -> r.get("checksum").asString(null));
			assertThat(checksums)
				.containsExactly(null, "1100083332", "3226785110", "1236540472", "18064555", "2663714411", "2581374719", "200310393",
						"949907516", "949907516", "2884945437", "1491717096", "227047158");
		}
	}
}
