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
package ac.simons.neo4j.migrations.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Michael J. Simons
 */
class StaticJavaBasedMigrationDiscovererTest {

	@Test
	void scannerShouldWork() {

		var discoverer = StaticJavaBasedMigrationDiscoverer.from(List.of("ac.simons.neo4j.migrations.quarkus.runtime"));
		assertThat(discoverer.getMigrationClasses())
			.hasSize(1)
			.first()
			.isEqualTo(SomeMigration.class);
	}

	@Test
	void shouldFilterByLocation() {

		var discoverer = StaticJavaBasedMigrationDiscoverer.from(List.of("ac.simons.neo4j.migrations.quarkus.runtime"));
		assertThat(discoverer.getMigrationClasses()).hasSize(1);

		var context = Mockito.mock(MigrationContext.class);
		when(context.getConfig()).thenReturn(
			MigrationsConfig.builder().withPackagesToScan("ac.simons.neo4j.migrations.quarkus.runtime").build());
		assertThat(discoverer.discover(context)).hasSize(1);

		context = Mockito.mock(MigrationContext.class);
		when(context.getConfig()).thenReturn(
			MigrationsConfig.builder().withPackagesToScan("ac.simons.neo4j.migrations.quarkus.not-there").build());
		assertThat(discoverer.discover(context)).isEmpty();
	}

	static class SomeMigration implements JavaBasedMigration {

		@Override
		public void apply(MigrationContext context) {
			throw new UnsupportedOperationException();
		}
	}
}
