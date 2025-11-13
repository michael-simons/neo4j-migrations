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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Michael J. Simons
 */
class DiscoveryServiceTests {

	@Test
	void shouldDiscoverMigrationsInCorrectOrder() {

		MigrationContext context = spy(new DefaultMigrationContext(MigrationsConfig.builder()
			.withPackagesToScan("ac.simons.neo4j.migrations.core.test_migrations.changeset3",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset1",
					"ac.simons.neo4j.migrations.core.test_migrations.changeset2")
			.withLocationsToScan("classpath:/my/awesome/migrations")
			.build(), mock(Driver.class)));

		doReturn(new DefaultConnectionDetails(null, "5.9", null, null, null, null)).when(context)
			.getConnectionDetails();

		List<Migration> migrations = new DiscoveryService().findMigrations(context);
		assertThat(migrations).extracting(Migration::getOptionalDescription)
			.filteredOn(Optional::isPresent)
			.hasSize(16)
			.map(Optional::get)
			.containsExactly("FirstMigration", "AnotherMigration", "InnerMigration", "BondTheNameIsBond",
					"BondTheNameIsBondNew", "BondTheNameIsBondNewNew", "Create constraints", "Die halbe Wahrheit",
					"Die halbe Wahrheit neu", "Die halbe Wahrheit neu neu", "NichtsIstWieEsScheint",
					"NichtsIstWieEsScheintNeu", "NichtsIstWieEsScheintNeuNeu", "MirFallenKeineNamenEin",
					"WithCommentAtEnd", "AMigration");
	}

	@Test
	void shouldDiscoverCallbacksInSameDirectoryAsMigrations() {

		MigrationContext context = new DefaultMigrationContext(
				MigrationsConfig.builder().withLocationsToScan("classpath:/my/awesome/migrations").build(),
				mock(Driver.class));

		Map<LifecyclePhase, List<Callback>> callbacks = new DiscoveryService().findCallbacks(context);
		assertThat(callbacks).hasSize(2).containsOnlyKeys(LifecyclePhase.BEFORE_MIGRATE, LifecyclePhase.AFTER_MIGRATE);
	}

	@Test
	void shouldMergeAndSortCallbacks() throws IOException {

		List<File> files = new ArrayList<>();

		File dir = Files.createTempDirectory("neo4j-migrations").toFile();
		files.add(new File(dir, "V1__One.cypher"));
		files.add(new File(dir, "afterMigrate"));
		for (File file : files) {
			file.createNewFile();
		}

		MigrationContext context = new DefaultMigrationContext(MigrationsConfig.builder()
			.withLocationsToScan("classpath:/my/awesome/migrations", "classpath:/my/awesome/callbacks",
					"file:" + dir.getAbsolutePath())
			.build(), mock(Driver.class));

		Map<LifecyclePhase, List<Callback>> callbacks = new DiscoveryService().findCallbacks(context);
		assertThat(callbacks).hasSize(LifecyclePhase.values().length);

		assertThat(callbacks.get(LifecyclePhase.AFTER_MIGRATE)).hasSize(4)
			.map(CypherBasedCallback.class::cast)
			.map(CypherBasedCallback::getSource)
			.containsExactly("afterMigrate.cypher", "afterMigrate.cypher", "afterMigrate__001.cypher",
					"afterMigrate__002.cypher");

		assertThat(callbacks.get(LifecyclePhase.BEFORE_FIRST_USE)).hasSize(2)
			.map(Callback::getSource)
			.containsExactly("beforeFirstUse.cypher", "beforeFirstUse__anotherStep.cypher");
	}

}
