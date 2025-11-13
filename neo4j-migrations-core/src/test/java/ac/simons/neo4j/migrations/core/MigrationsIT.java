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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ac.simons.neo4j.migrations.core.MigrationChain.ChainBuilderMode;
import ac.simons.neo4j.migrations.core.MigrationsConfig.CypherVersion;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogDiff;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Normalize;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;
import ac.simons.neo4j.migrations.core.refactorings.Rename;
import ac.simons.neo4j.migrations.core.test_migrations.changeset5.V003__Repeatable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.types.Node;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Michael J. Simons
 */
class MigrationsIT extends TestBase {

	private static List<File> createMigrationFiles(int n, File dir) throws IOException {
		return createMigrationFiles(n, 0, dir);
	}

	private static List<File> createMigrationFiles(int n, int offset, File dir) throws IOException {
		return createMigrationFiles(n, offset, dir, false);
	}

	private static List<File> createMigrationFiles(int n, int offset, File dir, boolean leadingZeros)
			throws IOException {
		List<File> files = new ArrayList<>();

		String format = leadingZeros ? "V%02d__Some.cypher" : "V%d__Some.cypher";

		for (int i = 1; i <= n; ++i) {
			File aFile = new File(dir, String.format(format, i + offset));
			aFile.createNewFile();
			files.add(aFile);
			Files.write(aFile.toPath(), Collections.singletonList("MATCH (n) RETURN n"));
		}
		return files;
	}

	static boolean isUsingLTSNeo4j() {
		for (var versionUnderTest : List.of("neo4j:4", "neo4j:5")) {
			if (TestBase.DEFAULT_NEO4J_IMAGE.contains(versionUnderTest)) {
				return true;
			}
		}
		return false;
	}

	static Stream<Arguments> lockSkippingShouldWork() {
		return Stream.of(
				Arguments.of((Consumer<Migrations>) Migrations::validate,
						(Consumer<Migrations>) migrations -> migrations.validate(false)),
				Arguments.of((Consumer<Migrations>) Migrations::info,
						(Consumer<Migrations>) migrations -> migrations.info(false)));
	}

	@Test
	void shouldApplyMigrations() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.build(), this.driver);
		migrations.apply();

		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(2);

		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), this.driver);
		migrations.apply();

		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(5);

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements()).hasSizeGreaterThan(0)
			.allMatch(element -> element.getState() == MigrationState.APPLIED);
		assertThat(migrationChain.getLastAppliedVersion()).hasValue(MigrationVersion.withValue("023.1.1"));
	}

	@Test
	void getLastAppliedMigrationShouldBeCorrect() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), this.driver);

		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.getElements()).hasSize(5);
		assertThat(migrationChain.getLastAppliedVersion()).isEmpty();
	}

	@Test // GH-573
	void shouldIgnoreNullRefactorings() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		Counters counters = migrations.apply((Refactoring[]) null);
		assertThat(counters).isEqualTo(Counters.empty());
	}

	@Test // GH-573
	void shouldIgnoreNullRefactoring() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		Counters counters = migrations.apply((Refactoring) null);
		assertThat(counters).isEqualTo(Counters.empty());
	}

	@Test // GH-573
	void shouldIgnoreEmptyRefactorings() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		Counters counters = migrations.apply(new Refactoring[0]);
		assertThat(counters).isEqualTo(Counters.empty());
	}

	@Test // GH-573
	void shouldApplyRefactorings() {

		try (Session session = this.driver.session()) {
			session.run("CREATE (m:Person {name:'Michael'}) -[:LIKES]-> (n:Person {name:'Tina', klug:'ja'})");
		}

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		Counters counters = migrations.apply(Rename.type("LIKES", "MAG"),
				Normalize.asBoolean("klug", Collections.singletonList("ja"), Collections.singletonList("nein")));
		assertThat(counters.propertiesSet()).isOne();
		assertThat(counters.typesRemoved()).isOne();
		assertThat(counters.typesAdded()).isOne();

		try (Session session = this.driver.session()) {
			long cnt = session
				.run("MATCH (m:Person {name:'Michael'}) -[:MAG]-> (n:Person {name:'Tina', klug: true}) RETURN count(m)")
				.single()
				.get(0)
				.asLong();
			assertThat(cnt).isOne();
		}
	}

	@Test // GH-573
	void shouldIgnoreNullResources() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		int cnt = migrations.apply((URL[]) null);
		assertThat(cnt).isZero();
	}

	@Test // GH-573
	void shouldIgnoreNullResource() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		int cnt = migrations.apply((URL) null);
		assertThat(cnt).isZero();
	}

	@Test // GH-573
	void shouldIgnoreEmptyResources() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		int cnt = migrations.apply(new URL[0]);
		assertThat(cnt).isZero();
	}

	@Test // GH-573
	void shouldFailProperOnInvalidFileName() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		URL resource = Objects
			.requireNonNull(MigrationsIT.class.getResource("/manual_resources/invalid_filename.cypher"));
		assertThatIllegalArgumentException().isThrownBy(() -> migrations.apply(resource))
			.withMessage(
					"Invalid name `%s`; the names of resources that should be applied must adhere to well-formed migration versions",
					resource.getPath());
	}

	@Test // GH-573
	void shouldFailProperOnUnsupportedExt() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		URL resource = Objects
			.requireNonNull(MigrationsIT.class.getResource("/manual_resources/V000__Invalid.extension"));
		assertThatIllegalArgumentException().isThrownBy(() -> migrations.apply(resource))
			.withMessage("Unsupported extension: extension");
	}

	@Test
	void shouldBeAwareThatThingsHaveBeenApplied() {
		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan("classpath:doublewillfail").build(),
				this.driver);
		assertThat(migrations.apply()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo("0001"));
		MigrationChain info = migrations.info();
		assertThat(migrations.apply()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo("0001"));
		MigrationChain info2 = migrations.info();
		assertThat(info.getElements()).first()
			.satisfies(e -> assertThat(e.getInstalledOn())
				.isEqualTo(info2.getElements().iterator().next().getInstalledOn()));
	}

	@Test
	void shouldFailIfNoMigrationsAreDiscoveredButThingsAreInsideTheDatabase() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), this.driver);
		migrations.apply();
		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(5);

		migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::info)
			.withMessage("More migrations have been applied to the database than locally resolved.");
	}

	@Test
	void localOnlyInfoShouldWork() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), this.driver);
		migrations.apply();

		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(5);

		MigrationChain migrationChain = migrations.info(ChainBuilderMode.LOCAL);
		assertThat(migrationChain.getElements()).isNotEmpty()
			.allMatch(element -> element.getState() == MigrationState.PENDING);
	}

	@Test
	void remoteOnlyInfoShouldWork() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.build(), this.driver);
		migrations.apply();
		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(5);

		migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		MigrationChain migrationChain = migrations.info(ChainBuilderMode.REMOTE);
		assertThat(migrationChain.getElements()).isNotEmpty()
			.allMatch(element -> element.getState() == MigrationState.APPLIED);
	}

	@Test
	void shouldRecordExecutionTime() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset4")
			.build(), this.driver);
		migrations.apply();

		try (Session session = this.driver.session()) {
			long executionTime = session.run(
					"MATCH (:__Neo4jMigration {version: 'BASELINE'}) -[r:MIGRATED_TO]->() RETURN r.in AS executionTime")
				.single()
				.get("executionTime")
				.asIsoDuration()
				.nanoseconds();
			assertThat(executionTime).isGreaterThanOrEqualTo(500000000L);
		}
	}

	@Test
	void shouldNotTryToImpersonateWithEmptyName() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset4")
			.withImpersonatedUser("  	 ")
			.build(), this.driver);
		assertThat(migrations.info()).isNotNull();
	}

	@Test
	void shouldFailWithNewMigrationsInBetween() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset3")
			.build(), this.driver);
		migrations.apply();

		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(1);

		Migrations failingMigrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
			.build(), this.driver);

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
			.withMessage("Unexpected migration at index 0: 001 (\"FirstMigration\").");
	}

	@Test
	void shouldNotSwallowMigrationExceptions() {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder().withLocationsToScan("classpath:broken").build(),
				this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withCauseInstanceOf(ClientException.class);
	}

	@Test
	void shouldFailWithChangedMigrations() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(2);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			Migrations failingMigrations = new Migrations(configuration, this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
				.withMessage("Checksum of 2 (\"Some\") changed!");
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2, 3 })
	void deleteMigrationsShouldWork(int fileNumber) throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(4, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, this.driver);
			assertThatNoException().isThrownBy(migrations::apply);

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(4);

			files.get(fileNumber).delete();

			// Make sure we fail consistently
			var failingMigrations = new Migrations(configuration, this.driver);
			var validationResult = failingMigrations.validate();
			assertThat(validationResult.getOutcome()).isEqualTo(ValidationResult.Outcome.INCOMPLETE_MIGRATIONS);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
				.withMessage("More migrations have been applied to the database than locally resolved.");

			var versionValue = Integer.toString(fileNumber + 1);
			var result = failingMigrations.delete(MigrationVersion.withValue(versionValue));
			assertThat(result.isDatabaseChanged()).isTrue();
			assertThat(result.getVersion()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo(versionValue));
			assertThat(result.getNodesDeleted()).isEqualTo(1L);
			if (fileNumber == files.size() - 1) {
				assertThat(result.getRelationshipsDeleted()).isEqualTo(1L);
				assertThat(result.getRelationshipsCreated()).isZero();
			}
			else {
				assertThat(result.getRelationshipsDeleted()).isEqualTo(2L);
				assertThat(result.getRelationshipsCreated()).isEqualTo(1L);
			}

			validationResult = failingMigrations.validate();
			assertThat(validationResult.getOutcome()).isEqualTo(ValidationResult.Outcome.VALID);
			assertThatNoException().isThrownBy(failingMigrations::apply);
			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(3);
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test
	void shouldNotFailWhenDeletingNonExistingVersion() {

		var migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		var result = migrations.delete(MigrationVersion.withValue("whatever"));
		assertThat(result.isDatabaseChanged()).isFalse();
		assertThat(result.getVersion()).isEmpty();
	}

	@Test // GH-702
	void shouldNotAllowChangingRepeatableType1() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(3, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(3);

			File renamedFile = new File(dir, "R" + 2 + "__Some.cypher");
			if (!files.get(1).renameTo(renamedFile)) {
				Assertions.fail("Could not rename file");
			}
			files.set(1, renamedFile);

			Migrations failingMigrations = new Migrations(configuration, this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
				.withMessage("State of 2 (\"Some\") changed from non-repeatable to repeatable");
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test // GH-702
	void shouldNotAllowChangingRepeatableType2() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(3, dir);

		File renamedFile = new File(dir, "R" + 2 + "__Some.cypher");
		if (!files.get(1).renameTo(renamedFile)) {
			Assertions.fail("Could not rename file");
		}
		File oldFile = files.set(1, renamedFile);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(3);

			if (!files.get(1).renameTo(oldFile)) {
				Assertions.fail("Could not rename file");
			}
			files.set(1, oldFile);

			Migrations failingMigrations = new Migrations(configuration, this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(failingMigrations::apply)
				.withMessage("State of 2 (\"Some\") changed from repeatable to non-repeatable");
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test // GH-702
	void shouldNotFailChangesInRepeatableMigrations() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(3, dir);

		File renamedFile = new File(dir, "R" + 2 + "__Some.cypher");
		if (!files.get(1).renameTo(renamedFile)) {
			Assertions.fail("Could not rename file");
		}
		files.set(1, renamedFile);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(3);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			migrations = new Migrations(configuration, this.driver);
			assertThatNoException().isThrownBy(migrations::apply);

			try (Session session = this.driver.session()) {
				long cnt = session.run("MATCH (m:SomeNode) RETURN count(m)").single().get(0).asLong();
				assertThat(cnt).isOne();
			}

			MigrationChain chain = migrations.info();
			assertThat(chain.getElements()).satisfiesExactly(
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("1", Optional.of("2648307716")),
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("2", Optional.of("1179729683")),
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("3", Optional.of("2648307716")));
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@Test // GH-702
	void shouldBuildGoodChainInTheMiddle() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		File renamedFile = new File(dir, "R" + 2 + "__Some.cypher");
		if (!files.get(1).renameTo(renamedFile)) {
			Assertions.fail("Could not rename file");
		}
		files.set(1, renamedFile);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder().withLocationsToScan(location).build();

			// First apply 2 migrations
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();
			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(2);

			// Change one, add 2 more and apply
			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));
			files.addAll(createMigrationFiles(2, files.size(), dir));
			migrations = new Migrations(configuration, this.driver);
			assertThatNoException().isThrownBy(migrations::apply);
			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(4);

			// Change repeatable once more and apply
			Files.write(files.get(1).toPath(), Collections.singletonList("CREATE (n:SomeNode) RETURN n;"));
			migrations = new Migrations(configuration, this.driver);
			assertThatNoException().isThrownBy(migrations::apply);
			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(4);

			MigrationChain chain = migrations.info();
			assertThat(chain.getElements()).satisfiesExactly(
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("1", Optional.of("2648307716")),
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("2", Optional.of("1674913780")),
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("3", Optional.of("2648307716")),
					e -> assertThat(e)
						.extracting(MigrationChain.Element::getVersion, MigrationChain.Element::getChecksum)
						.containsExactly("4", Optional.of("2648307716")));
		}
		finally {
			for (File file : files) {
				file.delete();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test // GH-702
	void shouldReapplyRepeatableJavaMigrations()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset5")
			.build(), this.driver);

		var getMigrations = Migrations.class.getDeclaredMethod("getMigrations");
		getMigrations.setAccessible(true);
		List<Migration> m = (List<Migration>) getMigrations.invoke(migrations);

		migrations.apply();

		((V003__Repeatable) m.get(2)).setChecksum(UUID.randomUUID().toString());
		migrations.apply();

		try (Session session = this.driver.session()) {
			long cnt = session.run("MATCH (m:FromRepeatedMig) RETURN count(m)").single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);

			cnt = session.run("MATCH (m:V003__Repeatable) RETURN count(m)").single().get(0).asLong();
			assertThat(cnt).isEqualTo(2L);

			cnt = session.run("MATCH (m:V004__Standard) RETURN count(m)").single().get(0).asLong();
			assertThat(cnt).isOne();
		}

		MigrationChain chain = migrations.info();
		assertThat(chain.getElements()).hasSize(4)
			.extracting(MigrationChain.Element::getType)
			.containsOnly(MigrationType.JAVA);

		((V003__Repeatable) m.get(2)).setRepeatable(false);

		// Should not change type, though
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withMessage("State of 003 (\"Repeatable\") changed from repeatable to non-repeatable");
	}

	@Test // GH-237
	void changedMigrationsShouldBeAllowedWhenValidateIsOff() throws IOException {

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		List<File> files = createMigrationFiles(2, dir);

		try {
			String location = "file:" + dir.getAbsolutePath();
			MigrationsConfig configuration = MigrationsConfig.builder()
				.withLocationsToScan(location)
				.withValidateOnMigrate(false)
				.build();
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(2);

			Files.write(files.get(1).toPath(), Arrays.asList("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(),
					Collections.singletonList("CREATE INDEX node_index_name FOR (n:Person) ON (n.surname)"));

			migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			try (Session session = this.driver.session()) {
				String version = session
					.run("MATCH (:__Neo4jMigration {version: '2'}) -[r:MIGRATED_TO]->(t) RETURN t.version AS version")
					.single()
					.get("version")
					.asString();
				assertThat(version).isEqualTo("3");
			}
		}
		finally {
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
			Migrations migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(2);

			File newMigration = new File(dir, "V3__SomethingNew.cypher");
			files.add(newMigration);
			Files.write(newMigration.toPath(), Collections.singletonList("MATCH (n) RETURN n"));

			migrations = new Migrations(configuration, this.driver);
			migrations.apply();

			assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(3);
		}
		finally {
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
		Migrations migrations = new Migrations(configuration, this.driver);

		Optional<MigrationVersion> finalVersion = migrations.apply();
		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(1);
		assertThat(finalVersion).flatMap(MigrationVersion::getOptionalDescription).hasValue("Just a couple of matches");

		configuration = MigrationsConfig.builder().withLocationsToScan("classpath:ml/unix").withAutocrlf(true).build();
		migrations = new Migrations(configuration, this.driver);

		finalVersion = migrations.apply();
		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(1);
		assertThat(finalVersion).flatMap(MigrationVersion::getOptionalDescription).hasValue("Just a couple of matches");
	}

	@Test
	void shouldNotFailOnDisabledAuth() {

		try (Neo4jContainer containerWithoutAuth = new Neo4jContainer(TestBase.DEFAULT_NEO4J_IMAGE)
			.withoutAuthentication()) {
			containerWithoutAuth.start();

			try (Driver driverWithoutAuth = GraphDatabase.driver(containerWithoutAuth.getBoltUrl())) {

				MigrationsConfig config = MigrationsConfig.builder()
					.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset1")
					.build();
				Migrations migrations = new Migrations(config, driverWithoutAuth);

				assertThat(migrations.apply()).map(MigrationVersion::getValue).hasValue("002");

				MigrationChain info = migrations.info();
				assertThat(info.getUsername()).isEqualTo("anonymous");
				assertThat(info.getElements()).element(1)
					.satisfies(e -> assertThat(e.getInstalledBy())
						.hasValue(System.getProperty("user.name") + "/anonymous"));
			}
		}
	}

	@Test // GH-647
	void shouldFailAsGracefullyAsItGetsWhenEditionMismatch() {

		MigrationsConfig configuration = MigrationsConfig.builder()
			.withLocationsToScan("classpath:ee")
			.withAutocrlf(true)
			.build();
		Migrations migrations = new Migrations(configuration, this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withMessage("Migration `01 (\"Use some enterprise features\")` uses a constraint that requires "
					+ "Neo4j Enterprise Edition but the database connected to is a Community edition, "
					+ "you might want to add a guard like `// assume that edition is enterprise` in your script");
	}

	@Test
	void shouldPreventDoubleVersionsJava() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset6")
			.build(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withMessage(
					"Duplicate version '001' (ac.simons.neo4j.migrations.core.test_migrations.changeset6.V001__FirstMigration, ac.simons.neo4j.migrations.core.test_migrations.changeset6.V001__SecondMigration)");
	}

	@Test // GH-725
	void shouldPreventDoubleVersionsCypher() {

		Migrations migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:duplicate").build(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withMessage("Duplicate version '001' (V001__Hallo.cypher, V001__Hello.cypher)");
	}

	@Test // GH-725
	void shouldNotPreventDoubleVersionsCypherWithPreconditions() {

		Migrations migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:duplicate-w-preconditions").build(),
				this.driver);
		assertThatNoException().isThrownBy(migrations::apply);
	}

	Stream<Arguments> shouldApplyResourceBasedMigrations() {
		var builder = Stream.<Arguments>builder();
		for (var transactionMode : TransactionMode.values()) {
			for (var cypherVersion : CypherVersion.values()) {
				builder.add(Arguments.of(transactionMode, cypherVersion));
			}
		}
		return builder.build();
	}

	@ParameterizedTest
	@MethodSource
	void shouldApplyResourceBasedMigrations(TransactionMode transactionMode, CypherVersion cypherVersion) {

		Migrations migrations;
		migrations = new Migrations(MigrationsConfig.builder()
			.withTransactionMode(transactionMode)
			.withCypherVersion(cypherVersion)
			.withLocationsToScan("classpath:my/awesome/migrations", "classpath:some/changeset")
			.build(), this.driver);

		Catalog localCatalog = migrations.getLocalCatalog();
		assertThat(localCatalog.getItems()).hasSize(2);
		Catalog databaseCatalog = migrations.getDatabaseCatalog();
		assertThat(databaseCatalog.getItems()).isEmpty();
		CatalogDiff diff = CatalogDiff.between(databaseCatalog, localCatalog);
		assertThat(diff.getItemsOnlyInRight()).containsAll(localCatalog.getItems());

		migrations.apply();

		assertThat(lengthOfMigrations(this.driver, null)).isEqualTo(12);

		databaseCatalog = migrations.getDatabaseCatalog();
		assertThat(databaseCatalog.getItems()).hasSize(1);
		diff = CatalogDiff.between(databaseCatalog, localCatalog);
		assertThat(diff.getItemsOnlyInRight()).map(CatalogItem::getName)
			.containsExactly(Name.of("constraint_with_options"));

		try (Session session = this.driver.session()) {
			String prop = session.run("MATCH (s:Stuff) RETURN s.prop").single().get(0).asString();
			String value = """

					this is a nice string with
					// a comment
					  // in it!
					""";
			assertThat(prop).isEqualTo(value);

			List<String> checksums = session.run(
					"MATCH (m:__Neo4jMigration) RETURN m.checksum AS checksum ORDER BY CASE WHEN m.version = 'BASELINE' THEN '0000' ELSE m.version END ASC")
				.list(r -> r.get("checksum").asString(null));
			assertThat(checksums).containsExactly(null, "1100083332", "3226785110", "1236540472", "18064555",
					"2663714411", "2581374719", "200310393", "949907516", "949907516", "2884945437", "1491717096",
					isModernNeo4j(migrations.getConnectionDetails()) ? "454777450" : "227047158");
		}
	}

	@ParameterizedTest // GH-719, GH-1537
	@ValueSource(ints = { 1, 2, 3, 4 })
	void shouldAllowCallInTX(int idx) {

		try (Session session = this.driver.session()) {
			session.run("with range(1,100) as r unwind r as i create (n:F) return n").consume();
			session.run("CREATE (a:Asset)-[:ASSET_TYPE]->(type:AssetType)");
		}

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		int appliedMigrations = migrations.apply(Objects.requireNonNull(
				MigrationsIT.class.getResource("/manual_resources/V000__Use_call_in_tx" + idx + ".cypher")));

		assertThat(appliedMigrations).isEqualTo(1);
	}

	@Test // GH-1342
	void addingMigrationsAfterRepeatingOnesMustNotFail() {

		var config = MigrationsConfig.builder()
			.withLocationsToScan("classpath:repeatable/original", "classpath:repeatable/constant")
			.build();

		var migrations = new Migrations(config, this.driver);
		migrations.clean(false);
		migrations.info();
		migrations.apply();

		config = MigrationsConfig.builder()
			.withLocationsToScan("classpath:repeatable/modified", "classpath:repeatable/constant",
					"classpath:repeatable/added")
			.build();

		migrations = new Migrations(config, this.driver);
		migrations.apply();

		// If chain is wrong, this would fail
		assertThatNoException().isThrownBy(migrations::apply);

		var records = this.driver.executableQuery("""
				MATCH (:`__Neo4jMigration` {version:'BASELINE'}) (()-[:MIGRATED_TO]->()){4} (t:`__Neo4jMigration`)
				RETURN t.version AS version
				""").execute().records();
		assertThat(records).hasSize(1).first().extracting(r -> r.get("version").asString()).isEqualTo("004");

		var repetitionCnt = this.driver
			.executableQuery("MATCH (n:`__Neo4jMigration` {version: '001'})-[r:REPEATED]->(n) RETURN count(r) AS cnt")
			.execute()
			.records()
			.get(0)
			.get("cnt")
			.asLong();
		assertThat(repetitionCnt).isOne();
	}

	@Test // GH-1428
	void shouldRespectTimeoutWithPerStatementTransactions() {

		var migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("classpath:sleepy")
			.withTransactionMode(TransactionMode.PER_STATEMENT)
			.withTransactionTimeout(Duration.ofMillis(500))
			.build(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withCauseInstanceOf(ClientException.class)
			.withStackTraceContaining(
					"The transaction has not completed within the timeout specified at its start by the client.");
	}

	@Test // GH-1428
	void shouldRespectTimeoutWithPerMigrationTransactions() {

		var migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("classpath:sleepy")
			.withTransactionMode(TransactionMode.PER_MIGRATION)
			.withTransactionTimeout(Duration.ofMillis(500))
			.build(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withCauseInstanceOf(ClientException.class)
			.withStackTraceContaining(
					"The transaction has not completed within the timeout specified at its start by the client.");
	}

	@Test // GH-1428
	void timeoutCanBeResetToDefault() {

		var migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan("classpath:sleepy")
			.withTransactionMode(TransactionMode.PER_MIGRATION)
			.withTransactionTimeout(Duration.ofMillis(10))
			.withTransactionTimeout(null)
			.build(), this.driver);
		assertThatNoException().isThrownBy(migrations::apply);
	}

	@Test // GH-1476
	@EnabledIf("isUsingLTSNeo4j")
	void shouldCreatedRelationshipIndex() {

		var migrations = new Migrations(MigrationsConfig.defaultConfig(), this.driver);
		assertThatNoException().isThrownBy(migrations::apply);

		try (var session = this.driver.session()) {
			var indexes = session.run("SHOW INDEXES yield name").list(record -> record.get("name").asString());
			assertThat(indexes).contains("repeated_at__Neo4jMigration");
		}
	}

	@ParameterizedTest // GH-1582
	@MethodSource
	void lockSkippingShouldWork(Consumer<Migrations> hard, Consumer<Migrations> soft) {
		var migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:softvalidations").build(), this.driver);

		// The single migration in the above resources contains an apoc.util.sleep call
		var thread = new Thread(migrations::apply);
		thread.start();

		try {
			// Wait on this thread long enough so that the locking thread can start
			Thread.sleep(1000);
			// hard validate
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> hard.accept(migrations))
				.withMessage(
						"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed");

			// soft validate
			assertThatNoException().isThrownBy(() -> soft.accept(migrations));

			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OrderingAndStopping {

		static final String FIND_NODES_QUERY = "MATCH (n:OOO) RETURN n ORDER BY n.created_on";

		private static MigrationsConfig.Builder defaultConfigPart() {
			return MigrationsConfig.builder().withTransactionMode(TransactionMode.PER_MIGRATION);
		}

		private static void assertChainOrder(Migrations migrations, String... expectedVersions) {
			assertChainOrder(migrations, Set.of(), expectedVersions);
		}

		private static void assertChainOrder(Migrations migrations, Set<String> pending, String... expectedVersions) {
			var chain = migrations.info();
			assertThat(chain.getElements()).allSatisfy(c -> {
				var isPending = pending.contains(c.getVersion());
				if (isPending) {
					assertThat(c.getState()).isEqualTo(MigrationState.PENDING);
					assertThat(c.getInstalledOn()).isEmpty();
				}
				else {
					assertThat(c.getState()).isEqualTo(MigrationState.APPLIED);
					assertThat(c.getInstalledOn()).isNotEmpty();
				}
			}).map(MigrationChain.Element::getVersion).containsExactly(expectedVersions);
		}

		@Test // GH-1213
		void shouldDefaultToOrderAndOrderShouldBeRight() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "005", "010", "015", "020");
			assertCreationOrder("N4", "N1", "N3", "N2");
		}

		@Test // GH-1213
		void shouldFailOnOutOfOrderByDefault() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(
					defaultConfigPart().withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/second")
						.build(),
					MigrationsIT.this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
				.withMessage("Unexpected migration at index 1: 015 (\"NewSecond\").");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderInTheBeginningOne() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(
					defaultConfigPart().withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/third")
						.withOutOfOrderAllowed(true)
						.build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "005", "010", "020");
			assertCreationOrder("N1", "N2", "N4");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderInTheBeginningN() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/third",
						"classpath:ooo/additional/third")
				.withOutOfOrderAllowed(true)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "001", "005", "009", "010", "020");
			assertCreationOrder("N1", "N2", "N7", "N4", "N8");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderInTheMiddleOne() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(
					defaultConfigPart().withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/second")
						.withOutOfOrderAllowed(true)
						.build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "015", "020");
			assertCreationOrder("N1", "N2", "N3");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderInTheMiddleN() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/second",
						"classpath:ooo/additional/second")
				.withOutOfOrderAllowed(true)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "014", "015", "019", "020");
			assertCreationOrder("N1", "N2", "N5", "N3", "N6");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderCombined() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:ooo/base/first", "classpath:ooo/base/second",
						"classpath:ooo/base/third")
				.withOutOfOrderAllowed(true)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "005", "010", "015", "020");
			assertCreationOrder("N1", "N2", "N4", "N3");
		}

		@Test // GH-1213
		void shouldNotFailOnOutOfOrderCombinedAll() {

			var migrations = new Migrations(defaultConfigPart().withLocationsToScan("classpath:ooo/base/first").build(),
					MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020");
			assertCreationOrder("N1", "N2");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:ooo/base", "classpath:ooo/additional", "classpath:ooo/repeatable/orig")
				.withOutOfOrderAllowed(true)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "001", "002", "005", "009", "010", "014", "015", "016", "019", "020");
			assertCreationOrder("N1", "N2", "N7", "N4", "N8", "N5", "N3", "N6");
			assertRepeats(2, 0);

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:ooo/base", "classpath:ooo/additional",
						"classpath:ooo/repeatable/modified")
				.withOutOfOrderAllowed(true)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "001", "002", "005", "009", "010", "014", "015", "016", "019", "020");
			assertCreationOrder("N1", "N2", "N7", "N4", "N8", "N5", "N3", "N6");
			assertRepeats(4, 2);
		}

		@Test // GH-1536
		void nonStop() {

			var migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v1", "classpath:stopping/pt3")
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020", "030", "040");
			assertCreationOrder("N1", "N2", "N3", "N4");
		}

		@Test // GH-1536
		void stopOnRepeat() {

			var migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v1", "classpath:stopping/pt3")
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020", "030", "040");
			assertCreationOrder("N1", "N2", "N3", "N4");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v2", "classpath:stopping/pt3",
						"classpath:stopping/pt4")
				.withTarget("R030")
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, Set.of("050", "060"), "010", "020", "030", "040", "050", "060");
			assertCreationOrder("N1", "N2", "N3", "N4", "N3R");
		}

		Stream<Arguments> stopOnVersion() {
			Consumer<Migrations> currentAssertion = migrations -> {
				assertChainOrder(migrations, Set.of("050", "060"), "010", "020", "030", "040", "050", "060");
				assertCreationOrder("N1", "N2", "N3", "N4");
			};

			Consumer<Migrations> latestAssertion = migrations -> {
				assertChainOrder(migrations, Set.of(), "010", "020", "030", "040", "050", "060");
				assertCreationOrder("N1", "N2", "N3", "N4", "N5", "N6");
			};

			Consumer<Migrations> nextAssertion = migrations -> {
				assertChainOrder(migrations, Set.of("060"), "010", "020", "030", "040", "050", "060");
				assertCreationOrder("N1", "N2", "N3", "N4", "N5");
			};

			return Stream.of(Arguments.of("current", currentAssertion), Arguments.of("V040", currentAssertion),
					Arguments.of("V045?", currentAssertion), Arguments.of("latest", latestAssertion),
					Arguments.of("V060", latestAssertion), Arguments.of("V100?", latestAssertion),
					Arguments.of("next", nextAssertion), Arguments.of("V050", nextAssertion),
					Arguments.of("V055?", nextAssertion));
		}

		@ParameterizedTest(name = "{0}") // GH-1536
		@MethodSource
		void stopOnVersion(String version, Consumer<Migrations> assertion) {

			var migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v1", "classpath:stopping/pt3")
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertChainOrder(migrations, "010", "020", "030", "040");
			assertCreationOrder("N1", "N2", "N3", "N4");

			migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v1", "classpath:stopping/pt3",
						"classpath:stopping/pt4")
				.withTarget(version)
				.build(), MigrationsIT.this.driver);
			migrations.apply();

			assertion.accept(migrations);
		}

		@Test // GH-1536
		void shouldAbortOnNonExisting() {

			var migrations = new Migrations(defaultConfigPart()
				.withLocationsToScan("classpath:stopping/pt1", "classpath:stopping/pt2v1", "classpath:stopping/pt3")
				.withTarget("V1000")
				.build(), MigrationsIT.this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
				.withMessage("Target version 1000 is not available");
		}

		private void assertCreationOrder(String... labels) {
			try (var session = MigrationsIT.this.driver.session()) {
				var nodes = session.executeRead(tx -> tx.run(FIND_NODES_QUERY).list(r -> r.get("n").asNode()));
				assertThat(nodes)
					.map(n -> StreamSupport.stream(n.labels().spliterator(), false)
						.filter(Predicate.not("OOO"::equals))
						.findFirst()
						.orElseThrow())
					.containsExactly(labels);
			}
		}

		private void assertRepeats(int expectedTotal, int expectedModified) {
			try (var session = MigrationsIT.this.driver.session()) {
				var nodes = session
					.executeRead(tx -> tx.run("MATCH (n:FromRepeatable) RETURN n").list(r -> r.get("n").asNode()));
				int cnt = 0;
				int mod = 0;
				for (Node node : nodes) {
					++cnt;
					for (String l : node.labels()) {
						if (l.equals("Mod")) {
							++mod;
						}
					}
				}
				assertThat(cnt).isEqualTo(expectedTotal);
				assertThat(mod).isEqualTo(expectedModified);
			}
		}

	}

	@Nested
	class Repairing {

		@Test
		void shouldCreateAProperChain(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir);

			List<File> files = new ArrayList<>();
			files.addAll(createMigrationFiles(1, 9, dir, true));
			files.addAll(createMigrationFiles(1, 19, dir, true));
			files.addAll(createMigrationFiles(1, 29, dir, true));
			files.addAll(createMigrationFiles(1, 39, dir, true));

			var migrations = new Migrations(locationAndConfig.config, MigrationsIT.this.driver);

			migrations.apply();

			var chain = migrations.info();
			assertThat(chain.getElements()).map(MigrationChain.Element::getVersion)
				.containsExactly("10", "20", "30", "40");
			assertThat(chain.getElements()).map(MigrationChain.Element::getChecksum)
				.map(Optional::get)
				.containsExactly("2648307716", "2648307716", "2648307716", "2648307716");

			Files.write(files.get(0).toPath(), List.of("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));
			assertThat(files.get(1).delete()).isTrue();
			assertThat(files.get(3).delete()).isTrue();

			files.addAll(createMigrationFiles(1, 4, dir, true));
			files.addAll(createMigrationFiles(1, 24, dir, true));
			var new40 = createMigrationFiles(1, 39, dir, true);
			files.addAll(new40);
			Files.write(new40.get(0).toPath(), List.of("MATCH (n) RETURN n;", "CREATE (m:Another) RETURN m;"));
			files.addAll(createMigrationFiles(1, 49, dir, true));
			migrations.clearCache();

			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply);
			migrations.repair();

			chain = migrations.info();
			assertThat(chain.getElements()).filteredOn(e -> e.getState() == MigrationState.APPLIED)
				.map(MigrationChain.Element::getVersion)
				.containsExactly("05", "10", "25", "30", "40");

			assertThat(chain.getElements()).filteredOn(e -> e.getState() == MigrationState.APPLIED)
				.map(MigrationChain.Element::getChecksum)
				.map(Optional::get)
				.containsExactly("2648307716", "1179729683", "2648307716", "2648307716", "1543575674");
		}

		@Test
		void repairingWithoutLocalMigrationsMustFail(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir);

			var migrations = new Migrations(locationAndConfig.config(), MigrationsIT.this.driver);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::repair)
				.withMessage(
						"Zero migrations have been discovered and repairing the database would lead to the deletion of all migrations recorded; if you want that, use the clean operation");
		}

		@Test
		void repairingWithoutRemoteMigrationsShouldNotDoAnything(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir, 2);

			var migrations = new Migrations(locationAndConfig.config(), MigrationsIT.this.driver);
			var result = migrations.repair();
			assertThat(result.getOutcome()).isEqualTo(RepairmentResult.Outcome.NO_REPAIRMENT_NECESSARY);
			assertThat(migrations.validate().isValid()).isFalse();
			assertThat(migrations.validate().getOutcome()).isEqualTo(ValidationResult.Outcome.INCOMPLETE_DATABASE);
		}

		@Test
		void shouldNotRepairAnythingWhenMigrationsAreAppended(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir, 2);
			var migrations = new Migrations(locationAndConfig.config, MigrationsIT.this.driver);

			migrations.apply();
			createMigrationFiles(1, locationAndConfig.generatedFiles().size(), dir);
			migrations.clearCache();

			var validationResult = migrations.validate();
			assertThat(validationResult.isValid()).isFalse();
			assertThat(validationResult.getOutcome()).isEqualTo(ValidationResult.Outcome.INCOMPLETE_DATABASE);

			var result = migrations.repair();
			assertThat(result.getOutcome()).isEqualTo(RepairmentResult.Outcome.NO_REPAIRMENT_NECESSARY);
		}

		@Test
		void shouldFixChangedChecksums(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir, 2);
			var migrations = new Migrations(locationAndConfig.config, MigrationsIT.this.driver);

			migrations.apply();

			Files.write(locationAndConfig.generatedFiles().get(1).toPath(),
					List.of("MATCH (n) RETURN n;", "CREATE (m:SomeNode) RETURN m;"));

			migrations.clearCache();
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply);

			var result = migrations.repair();
			assertThat(result.getOutcome()).isEqualTo(RepairmentResult.Outcome.REPAIRED);
			assertThat(result.getNodesDeleted()).isZero();
			assertThat(result.getRelationshipsCreated()).isZero();
			assertThat(result.getRelationshipsDeleted()).isZero();
			assertThat(result.getPropertiesSet()).isOne();

			assertThatNoException().isThrownBy(migrations::apply);
			assertThat(migrations.validate().isValid()).isTrue();
		}

		@SuppressWarnings("JUnitMalformedDeclaration") // Not true
		@ParameterizedTest
		@ValueSource(ints = { 0, 1, 2, 3 })
		void shouldFixMissingLocalMigrations(int position, @TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir, 4);
			var migrations = new Migrations(locationAndConfig.config, MigrationsIT.this.driver);

			migrations.apply();

			assertThat(locationAndConfig.generatedFiles().get(position).delete()).isTrue();

			migrations.clearCache();
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply);

			var result = migrations.repair();
			assertThat(result.getOutcome()).isEqualTo(RepairmentResult.Outcome.REPAIRED);
			assertThat(result.getNodesDeleted()).isOne();
			if (position != 3) {
				assertThat(result.getRelationshipsDeleted()).isEqualTo(2L);
				assertThat(result.getRelationshipsCreated()).isOne();
				assertThat(result.getPropertiesSet()).isEqualTo(4);
			}
			else {
				assertThat(result.getRelationshipsDeleted()).isOne();
				assertThat(result.getRelationshipsCreated()).isZero();
				assertThat(result.getPropertiesSet()).isZero();
			}

			var expectedVersions = IntStream.rangeClosed(1, 4)
				.filter(i -> i != position + 1)
				.mapToObj(Integer::toString)
				.toList();
			assertThat(migrations.info().getElements()).map(MigrationChain.Element::getVersion)
				.containsExactlyElementsOf(expectedVersions);
			assertThatNoException().isThrownBy(migrations::apply);
			assertThat(migrations.validate().isValid()).isTrue();
		}

		@Test
		void shouldInsertElements(@TempDir File dir) throws IOException {

			var locationAndConfig = LocationAndConfig.of(dir);
			var migrations = new Migrations(locationAndConfig.config, MigrationsIT.this.driver);

			createMigrationFiles(4, 10, dir);

			migrations.apply();

			createMigrationFiles(1, 0, dir);

			migrations.clearCache();
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply);

			var result = migrations.repair();
			assertThat(result.getOutcome()).isEqualTo(RepairmentResult.Outcome.REPAIRED);
			assertThat(result.getNodesDeleted()).isZero();
			assertThat(result.getNodesCreated()).isOne();
			assertThat(result.getRelationshipsDeleted()).isOne();
			assertThat(result.getRelationshipsCreated()).isEqualTo(2L);
			assertThat(result.getPropertiesSet()).isEqualTo(15L);

			assertThatNoException().isThrownBy(migrations::apply);
			assertThat(migrations.validate().isValid()).isTrue();

			var newChain = new Migrations(locationAndConfig.config, MigrationsIT.this.driver).info();
			assertThat(newChain.getElements()).map(MigrationChain.Element::getVersion)
				.containsExactly("1", "11", "12", "13", "14");
		}

		record LocationAndConfig(String location, MigrationsConfig config, List<File> generatedFiles) {

			static LocationAndConfig of(File dir) throws IOException {
				return of(dir, 0);
			}

			static LocationAndConfig of(File dir, int numFiles) throws IOException {
				var location = "file:" + dir.getAbsolutePath();
				var configuration = MigrationsConfig.builder().withLocationsToScan(location).build();
				return new LocationAndConfig(location, configuration,
						(numFiles == 0) ? List.of() : createMigrationFiles(numFiles, dir));
			}
		}

	}

}
