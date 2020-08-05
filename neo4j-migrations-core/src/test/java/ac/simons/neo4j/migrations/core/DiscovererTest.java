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

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.Discoverer.CypherBasedMigrationDiscoverer;
import ac.simons.neo4j.migrations.core.Discoverer.JavaBasedMigrationDiscoverer;
import ac.simons.neo4j.migrations.core.Migrations.DefaultMigrationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 */
class DiscovererTest {

	@Nested
	class JavaBasedMigrationDiscovererTest {
		@Test
		void shouldFindMigrations() {

			MigrationContext context = new DefaultMigrationContext(
				MigrationsConfig.builder().withPackagesToScan(
					"ac.simons.neo4j.migrations.core.test_migrations.changeset1").build(), Mockito.mock(Driver.class));

			Collection<Migration> migrations = new JavaBasedMigrationDiscoverer().discoverMigrations(context);
			assertThat(migrations).hasSize(2)
				.extracting(Migration::getDescription)
				.contains("FirstMigration", "AnotherMigration");
		}

		@Test
		void shouldFindStaticInnerClasses() {

			MigrationContext context = new DefaultMigrationContext(
				MigrationsConfig.builder().withPackagesToScan(
					"ac.simons.neo4j.migrations.core.test_migrations.changeset3").build(), Mockito.mock(Driver.class));

			Collection<Migration> migrations = new JavaBasedMigrationDiscoverer().discoverMigrations(context);
			assertThat(migrations).hasSize(1)
				.extracting(Migration::getDescription)
				.contains("InnerMigration");
		}
	}

	@Nested
	class CypherBasedMigrationDiscovererTest {

		@Test
		void shouldFindClasspathResources() {

			// Make sure we run in a Maven or CI build. When run via IDEA (and probably other IDEs, dependencies
			// are usually resolved via an internal reactor and thus not being JAR urls, which defeats the purpose
			// of this test.
			Assumptions
				.assumeThat(getClass().getResource("/some/changeset/V0001__delete_old_data.cypher").getProtocol())
				.isEqualTo("jar");

			MigrationContext context = new DefaultMigrationContext(
				MigrationsConfig.builder().withLocationsToScan(
					"classpath:my/awesome/migrations", "classpath:some/changeset").build(), Mockito.mock(Driver.class));

			Collection<Migration> migrations = new CypherBasedMigrationDiscoverer().discoverMigrations(context);
			assertThat(migrations).hasSize(5)
				.extracting(Migration::getDescription)
				.contains("delete old data", "create new data", "BondTheNameIsBond", "Die halbe Wahrheit",
					"MirFallenKeineNamenEin");
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
				MigrationContext context = new DefaultMigrationContext(
					MigrationsConfig.builder().withLocationsToScan(
						"file:" + dir.getAbsolutePath(), "file:" + dir2.getAbsolutePath()).build(),
					Mockito.mock(Driver.class));

				Collection<Migration> migrations = new CypherBasedMigrationDiscoverer().discoverMigrations(context);
				assertThat(migrations).hasSize(3)
					.extracting(Migration::getDescription)
					.contains("One", "Two", "Three");
			} finally {
				for (File file : files) {
					file.delete();
				}
				subDir.delete();
			}
		}
	}
}
