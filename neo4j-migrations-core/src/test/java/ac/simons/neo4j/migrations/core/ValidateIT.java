/*
 * Copyright 2020-2021 the original author or authors.
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

import ac.simons.neo4j.migrations.core.ValidationResult.Outcome;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

/**
 * @author Michael J. Simons
 */
class ValidateIT extends TestBase {

	@Test
	void noResolvedMigrationsShouldResultInValidWithWarning() {

		Migrations migrations = new Migrations(MigrationsConfig.builder().build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.VALID);
		assertThat(result.getWarnings()).containsExactly("No migrations resolved.");
	}

	@Test
	void actuallyValid() {

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		migrations.apply();
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.VALID);
		assertThat(result.getWarnings()).isEmpty();
	}

	@Test
	void noMigrationsThereAtAll() {

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.INCOMPLETE_DATABASE);
	}

	@Test
	void partiallyApplied() {

		try (Session session = driver.session()) {
			session.run(
					"CREATE (n:`__Neo4jMigration` {version:\"BASELINE\"}) -[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> (m:`__Neo4jMigration` {checksum:\"1100083332\",description:\"delete old data\",source:\"V0001__delete_old_data.cypher\",type:\"CYPHER\",version:\"0001\"})",
					Values.parameters("when", ZonedDateTime.now(), "duration", Duration.ofSeconds(23)))
				.consume();
		}

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.INCOMPLETE_DATABASE);
	}

	@Test
	void incompleteLocal() {

		try (Session session = driver.session()) {
			session.run(
					"CREATE (:`__Neo4jMigration` {version:\"BASELINE\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"1100083332\",description:\"delete old data\",source:\"V0001__delete_old_data.cypher\",type:\"CYPHER\",version:\"0001\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"3226785110\",description:\"create new data\",source:\"V0002__create_new_data.cypher\",type:\"CYPHER\",version:\"0002\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"3226785110\",description:\"create new data\",source:\"V0002__create_new_data.cypher\",type:\"CYPHER\",version:\"0003\"}) ",
					Values.parameters("when", ZonedDateTime.now(), "duration", Duration.ofSeconds(23)))
				.consume();
		}

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.INCOMPLETE_MIGRATIONS);
	}

	@Test
	void wrongChecksum() {

		try (Session session = driver.session()) {
			session.run(
					"CREATE (:`__Neo4jMigration` {version:\"BASELINE\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"1100083332\",description:\"delete old data\",source:\"V0001__delete_old_data.cypher\",type:\"CYPHER\",version:\"0001\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"3326785110\",description:\"create new data\",source:\"V0002__create_new_data.cypher\",type:\"CYPHER\",version:\"0002\"}) ",
					Values.parameters("when", ZonedDateTime.now(), "duration", Duration.ofSeconds(23)))
				.consume();
		}

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.DIFFERENT_CONTENT);
	}

	@Test
	void wrongVersion() {

		try (Session session = driver.session()) {
			session.run(
					"CREATE (:`__Neo4jMigration` {version:\"BASELINE\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"1100083332\",description:\"delete old data\",source:\"V0001__delete_old_data.cypher\",type:\"CYPHER\",version:\"0001\"}) "
					+ "-[:MIGRATED_TO {connectedAs:\"neo4j\",at:$when,in:$duration,by:\"msimons\"}]-> "
					+ "     (:`__Neo4jMigration` {checksum:\"3226785110\",description:\"create new data\",source:\"V0002__create_new_data.cypher\",type:\"CYPHER\",version:\"0003\"}) ",
					Values.parameters("when", ZonedDateTime.now(), "duration", Duration.ofSeconds(23)))
				.consume();
		}

		Migrations migrations = new Migrations(MigrationsConfig
			.builder().withLocationsToScan("/some/changeset")
			.build(), driver);
		ValidationResult result = migrations.validate();
		assertThat(result.getOutcome()).isEqualTo(Outcome.DIFFERENT_CONTENT);
	}
}
