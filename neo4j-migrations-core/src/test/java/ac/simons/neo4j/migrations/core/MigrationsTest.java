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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
class MigrationsTest extends TestBase {

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

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(3);

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements())
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
	void shouldFailWithNewMigrationsInBetween() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset3").build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(1);

		Migrations failingMigrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.build(), driver);

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> failingMigrations.apply())
			.withMessage("Unexpected migration at index 0: 001 (\"FirstMigration\")");
	}

	@Test
	void shouldFailWithChangedMigrations() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "filesystem:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			Migrations failingMigrations = new Migrations(configuration, driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> failingMigrations.apply())
				.withMessage("Checksum of 2 (\"Some\") changed!");
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
			String location = "filesystem:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(2);

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(), Arrays.asList("MATCH (n) RETURN n"));

			migrations = new Migrations(configuration, driver);
			migrations.apply();

			assertThat(lengthOfMigrations(driver, null)).isEqualTo(3);
		} finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	private static List<File> createMigrationFiles(int n, File dir) throws IOException {
		List<File> files = new ArrayList<>();
		for (int i = 1; i <= n; ++i) {
			File aFile = new File(dir, "V" + i + "__Some.cypher");
			aFile.createNewFile();
			files.add(aFile);
			Files.write(aFile.toPath(), Arrays.asList("MATCH (n) RETURN n"));
		}
		return files;
	}

	@Test
	void shouldApplyCypherBasedMigrations() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan(
			"classpath:my/awesome/migrations", "classpath:some/changeset").build(), driver);
		migrations.apply();

		assertThat(lengthOfMigrations(driver, null)).isEqualTo(5);

		try (Session session = driver.session()) {
			List<String> checksums = session.run("MATCH (m:__Neo4jMigration) RETURN m.checksum AS checksum")
				.list(r -> r.get("checksum").asString(null));
			assertThat(checksums)
				.containsExactly(null, "1100083332", "3226785110", "1236540472", "200310393", "2884945437");
		}
	}
}
