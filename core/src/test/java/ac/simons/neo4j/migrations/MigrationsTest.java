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
package ac.simons.neo4j.migrations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
class MigrationsTest extends TestBase {

	@Test
	void shouldFindMigrations() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1").build(), driver);

		List<Migration> listOfMigrations;
		listOfMigrations = migrations.findMigrations();
		Assertions.assertAll(
			() -> Assertions.assertEquals(2, listOfMigrations.size()),
			() -> Assertions.assertEquals("FirstMigration", listOfMigrations.get(0).getDescription()),
			() -> Assertions.assertEquals("AnotherMigration", listOfMigrations.get(1).getDescription())
		);
	}

	@Test
	void shouldFindStaticInnerClasses() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset3").build(), driver);

		List<Migration> listOfMigrations;
		listOfMigrations = migrations.findMigrations();
		Assertions.assertAll(
			() -> Assertions.assertEquals(1, listOfMigrations.size()),
			() -> Assertions.assertEquals("InnerMigration", listOfMigrations.get(0).getDescription())
		);
	}

	@Test
	void shouldFindClasspathResources() {

		// Make sure we run in a Maven or CI build. When run via IDEA (and probably other IDEs, dependencies
		// are usually resolved via an internal reactor and thus not being JAR urls, which defeats the purpose
		// of this test.

		Assumptions.assumeTrue(
			"jar".equals(getClass().getResource("/some/changeset/V0001__delete_old_data.cypher").getProtocol()));

		Migrations migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan(
			"classpath:my/awesome/migrations", "classpath:some/changeset").build(), driver);

		List<Migration> listOfMigrations = migrations.findMigrations();
		Assertions.assertAll(
			() -> Assertions.assertEquals(5, listOfMigrations.size()),
			() -> Assertions.assertEquals("delete old data", listOfMigrations.get(0).getDescription()),
			() -> Assertions.assertEquals("create new data", listOfMigrations.get(1).getDescription()),
			() -> Assertions.assertEquals("BondTheNameIsBond", listOfMigrations.get(2).getDescription()),
			() -> Assertions.assertEquals("Die halbe Wahrheit", listOfMigrations.get(3).getDescription()),
			() -> Assertions.assertEquals("MirFallenKeineNamenEin", listOfMigrations.get(4).getDescription())
		);
	}

	@Test
	void shouldFindFileSystemResources() throws IOException {

		List<File> files = new ArrayList<>();

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		File subDir = new File(dir, "subdir");
		subDir.mkdir();
		files.add(new File(dir, "V1__One.cypher"));
		files.add(new File(subDir, "V2__Two.cypher"));

		File dir2 = Files.createTempDirectory("neo4j-migrations2").toFile();
		files.add(new File(dir, "V3__Three.cypher"));

		for (File file : files) {
			file.createNewFile();
		}

		try {
			Migrations migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan(
				"filesystem:" + dir.getAbsolutePath(), "filesystem:" + dir2.getAbsolutePath()).build(), driver);

			List<Migration> listOfMigrations = migrations.findMigrations();
			Assertions.assertAll(
				() -> Assertions.assertEquals(3, listOfMigrations.size()),
				() -> Assertions.assertEquals("One", listOfMigrations.get(0).getDescription()),
				() -> Assertions.assertEquals("Two", listOfMigrations.get(1).getDescription()),
				() -> Assertions.assertEquals("Three", listOfMigrations.get(2).getDescription())
			);
		} finally {
			for (File file : files) {
				file.delete();
			}
			subDir.delete();
		}
	}

	@Test
	void shouldApplyMigrations() {

		clearDatabase();

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1").build(), driver);
		migrations.apply();

		Assertions.assertEquals(2, lengthOfMigrations());

		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1",
			"ac.simons.neo4j.migrations.test_migrations.changeset2")
			.build(), driver);
		migrations.apply();

		Assertions.assertEquals(3, lengthOfMigrations());
	}

	@Test
	void shouldFailWithNewMigrationsInBetween() {

		clearDatabase();

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset3").build(), driver);
		migrations.apply();

		Assertions.assertEquals(1, lengthOfMigrations());

		Migrations failingMigrations = new Migrations(MigrationsConfig.builder().withPackagesToScan(
			"ac.simons.neo4j.migrations.test_migrations.changeset1")
			.build(), driver);

		String msg;
		msg = Assertions.assertThrows(MigrationsException.class, () -> failingMigrations.apply()).getMessage();
		Assertions.assertEquals("Unexpected migration at index 0: 001 (\"FirstMigration\")", msg);
	}

	@Test
	void shouldFailWithChangedMigrations() throws IOException {

		clearDatabase();

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "filesystem:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			Assertions.assertEquals(2, lengthOfMigrations());

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			Migrations failingMigrations = new Migrations(configuration, driver);
			String msg;
			msg = Assertions.assertThrows(MigrationsException.class, () -> failingMigrations.apply()).getMessage();
			Assertions.assertEquals("Checksum of 2 (\"Some\") changed!", msg);
		} finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test
	void shouldVerifyChecksums() throws IOException {

		clearDatabase();

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "filesystem:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, driver);
			migrations.apply();

			Assertions.assertEquals(2, lengthOfMigrations());

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(), Arrays.asList("MATCH (n) RETURN n"));

			migrations = new Migrations(configuration, driver);
			migrations.apply();

			Assertions.assertEquals(3, lengthOfMigrations());
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

		clearDatabase();

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan(
			"classpath:my/awesome/migrations", "classpath:some/changeset").build(), driver);
		migrations.apply();

		Assertions.assertEquals(5, lengthOfMigrations());

		try (Session session = driver.session()) {
			List<String> checksums = session.run("MATCH (m:__Neo4jMigration) RETURN m.checksum AS checksum")
				.list(r -> r.get("checksum").asString());
			Assertions.assertArrayEquals(new String[] { "1100083332", "3226785110", "1236540472", "200310393", "2884945437" },
				checksums.subList(1, checksums.size()).toArray(new String[0]));
		}
	}

	void clearDatabase() {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
		}
	}

	int lengthOfMigrations() {
		try (Session session = driver.session()) {
			return session.run(""
				+ "MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:`__Neo4jMigration`) "
				+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) "
				+ "RETURN length(p) AS l").single().get("l").asInt();
		}
	}
}
