/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

/**
 * @author Michael J. Simons
 */
class ChangedPreconditionsIT extends TestBase {

	@Test // GH-558
	void adaptingToCypherChangesAfterTheFactShouldBePossible() {

		// First use old syntax, whatever. No assumptions in place
		Migrations migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:preconditions3/old").build(), this.driver);
		Optional<MigrationVersion> lastVersion = migrations.apply();
		Assertions.assertThat(lastVersion).hasValue(MigrationVersion.withValue("02"));

		try (Session session = this.driver.session()) {
			Set<String> allLabels = new HashSet<>();
			session.run("MATCH (n) WHERE NOT n:__Neo4jMigration return labels(n)").forEachRemaining(r -> {
				allLabels.addAll(r.get(0).asList(Value::asString));
			});
			Assertions.assertThat(allLabels).containsExactlyInAnyOrder("Old", "Both");
		}

		// Run a second time, with the changed folder structure.
		// Here, it is a different one, to make up for both test, but in reality, it would
		// be the same
		// Migrations must not fail
		// We will see the old migration being skipped (moved into the compat subfolder),
		// its checksum changed
		// but the new one (in current subfolder) will match with its alternative checksum
		migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:preconditions3/new").build(), this.driver);

		lastVersion = migrations.apply();
		Assertions.assertThat(lastVersion).hasValue(MigrationVersion.withValue("02"));

		// On a fresh database, the new one will be applied
		clearDatabase();

		migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:preconditions3/new").build(), this.driver);

		lastVersion = migrations.apply();
		Assertions.assertThat(lastVersion).hasValue(MigrationVersion.withValue("02"));
		try (Session session = this.driver.session()) {
			Set<String> allLabels = new HashSet<>();
			session.run("MATCH (n) WHERE NOT n:__Neo4jMigration return labels(n)").forEachRemaining(r -> {
				allLabels.addAll(r.get(0).asList(Value::asString));
			});
			Assertions.assertThat(allLabels).containsExactlyInAnyOrder("New", "Both");
		}
	}

}
