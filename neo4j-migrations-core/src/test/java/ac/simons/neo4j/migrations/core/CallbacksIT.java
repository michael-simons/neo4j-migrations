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

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.ClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
class CallbacksIT extends TestBase {

	private final MigrationsConfig config = MigrationsConfig.builder()
		.withLocationsToScan("classpath:callbacksit")
		.build();

	@Test
	void shouldCallBeforeFirstUseOnlyBeforeFirstUse() {

		Migrations migrations = new Migrations(this.config, this.driver);
		migrations.clean(false);
		migrations.info();
		migrations.apply();
		assertThat(count("BeforeFirstUser")).isOne();
	}

	@Test
	void callbacksAroundMigrateShouldWork() {

		Migrations migrations = new Migrations(this.config, this.driver);
		migrations.apply();
		assertThat(count("BeforeMigrate")).isOne();
		assertThat(countMigratedNodes("BeforeMigrate")).isZero();
		assertThat(count("AfterMigrate")).isOne();
		assertThat(countMigratedNodes("AfterMigrate")).isOne();
	}

	@Test
	void callbacksAroundInfoShouldWork() {

		Migrations migrations = new Migrations(this.config, this.driver);
		MigrationChain migrationChain = migrations.info();
		assertThat(migrationChain.isApplied("0001")).isTrue();
		assertThat(count("BeforeInfo")).isOne();
		assertThat(count("AfterInfo")).isOne();
	}

	@Test
	void callbacksAroundValidateShouldWork() {

		Migrations migrations = new Migrations(this.config, this.driver);
		ValidationResult validationResult = migrations.validate();
		assertThat(validationResult.isValid()).isTrue();
		assertThat(count("BeforeValidate")).isOne();
		assertThat(count("AfterValidate")).isOne();
	}

	@Test
	void callbacksAroundCleanShouldWork() {

		Migrations migrations = new Migrations(this.config, this.driver);
		CleanResult cleanResult = migrations.clean(true);
		assertThat(cleanResult.getNodesDeleted()).isEqualTo(2);
		assertThat(cleanResult.getRelationshipsDeleted()).isEqualTo(1);
		assertThat(count("BeforeClean")).isOne();
		assertThat(count("AfterClean")).isOne();
	}

	@Test
	void callbackExceptionsShouldBeWrapped() {

		Migrations migrations = new Migrations(
				MigrationsConfig.builder().withLocationsToScan("classpath:callbacksbroken").build(), this.driver);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
			.withMessage("Could not invoke beforeFirstUse callback.")
			.withCauseInstanceOf(ClientException.class)
			.withStackTraceContaining("Tried to execute Write query after executing Schema modification");
	}

	private long count(String label) {
		try (Session session = this.driver.session()) {
			return session
				.run("MATCH (n) WITH labels(n) AS labels WHERE size(labels) = 1 AND $label in labels RETURN count(*)",
						Values.parameters("label", label))
				.single()
				.get(0)
				.asLong();
		}
	}

	private long countMigratedNodes(String label) {
		try (Session session = this.driver.session()) {
			return session.run(
					"MATCH (n) WITH n, labels(n) AS labels WHERE size(labels) = 1 AND $label in labels RETURN n.migratedNodes",
					Values.parameters("label", label))
				.single()
				.get(0)
				.asLong();
		}
	}

}
