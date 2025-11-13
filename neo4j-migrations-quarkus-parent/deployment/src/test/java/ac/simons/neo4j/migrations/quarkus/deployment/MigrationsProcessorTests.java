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
package ac.simons.neo4j.migrations.quarkus.deployment;

import java.io.IOException;
import java.util.List;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.quarkus.runtime.StaticClasspathResourceScanner;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class MigrationsProcessorTests {

	@Test
	void featureShouldBeCorrect() {
		// FFS Sonar, that wasn't a new method.
		assertThat(new MigrationsProcessor().createFeature()).extracting(FeatureBuildItem::getName)
			.isEqualTo(MigrationsProcessor.FEATURE_NAME);
	}

	static class SomeMigration implements JavaBasedMigration {

		@Override
		public void apply(MigrationContext context) {
			throw new UnsupportedOperationException();
		}

	}

	@Nested
	class ClassDiscovering {

		@Test
		void emptyPackageShouldAbortEarly() {
			var classes = MigrationsProcessor.findClassBasedMigrations(List.of(), null);
			assertThat(classes).isEmpty();
		}

		@Test
		void shouldFindClasses() throws IOException {

			var indexer = new Indexer();
			indexer.indexClass(MigrationsProcessorTests.SomeMigration.class);

			var classes = MigrationsProcessor
				.findClassBasedMigrations(List.of(MigrationsProcessorTests.class.getPackageName()), indexer.complete());
			assertThat(classes).containsExactly(SomeMigration.class);
		}

		@Test
		void shouldNotSelectClassesWhenInDifferentPackage() throws IOException {

			var indexer = new Indexer();
			indexer.indexClass(MigrationsProcessorTests.SomeMigration.class);

			var classes = MigrationsProcessor.findClassBasedMigrations(List.of("whatever"), indexer.complete());
			assertThat(classes).isEmpty();
		}

		@Test
		void shouldIgnoreUnloadableClasses() throws IOException {

			var indexer = new Indexer();
			indexer.indexClass(MigrationsProcessorTests.SomeMigration.class);
			var ct = Thread.currentThread();
			var cl = ct.getContextClassLoader();
			try {

				ct.setContextClassLoader(new ClassLoader() {
					@Override
					public String getName() {
						return "dang";
					}

					@Override
					public Class<?> loadClass(String name) throws ClassNotFoundException {
						if (SomeMigration.class.getName().equals(name)) {
							throw new ClassNotFoundException(name);
						}
						return super.loadClass(name);
					}
				});
				var classes = MigrationsProcessor.findClassBasedMigrations(
						List.of(MigrationsProcessorTests.class.getPackageName()), indexer.complete());
				assertThat(classes).isEmpty();
			}
			finally {
				ct.setContextClassLoader(cl);
			}
		}

	}

	@Nested
	class ResourceScanning {

		@Test
		void scannerShouldWork() throws IOException {

			var resources = MigrationsProcessor.findResourceBasedMigrations(ResourceBasedMigrationProvider.unique(),
					List.of("classpath:static"));
			assertThat(resources).hasSize(1).first().satisfies(resource -> {
				assertThat(resource.getPath()).isEqualTo("static/arbitrary.cypher");
				assertThat(resource.getUrl()).isNotNull();
			});
		}

		@Test
		void shouldFilterByLocation() throws IOException {

			var resources = MigrationsProcessor.findResourceBasedMigrations(ResourceBasedMigrationProvider.unique(),
					List.of("classpath:static", "classpath:also"));
			assertThat(resources).hasSize(2);

			var scanner = StaticClasspathResourceScanner.of(resources);
			assertThat(scanner.scan(List.of("also"))).hasSize(1);
			assertThat(scanner.scan(List.of("static"))).hasSize(1);

			assertThat(scanner.scan(List.of("static", "also"))).hasSize(2);
			assertThat(scanner.scan(List.of())).isEmpty();
		}

		@Test
		void shouldStripLeadingSlash() throws IOException {

			var resources = MigrationsProcessor.findResourceBasedMigrations(ResourceBasedMigrationProvider.unique(),
					List.of("classpath:/static", "classpath:also"));
			assertThat(resources).hasSize(2);

			var scanner = StaticClasspathResourceScanner.of(resources);
			assertThat(scanner.scan(List.of("also"))).hasSize(1);
			assertThat(scanner.scan(List.of("/static"))).hasSize(1);

			assertThat(scanner.scan(List.of("/static", "also"))).hasSize(2);
			assertThat(scanner.scan(List.of())).isEmpty();
		}

	}

}
