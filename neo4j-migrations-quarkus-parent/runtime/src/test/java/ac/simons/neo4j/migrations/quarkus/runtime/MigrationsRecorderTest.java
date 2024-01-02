/*
 * Copyright 2020-2024 the original author or authors.
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

import ac.simons.neo4j.migrations.core.MigrationsConfig;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrationsRecorderTest {

	@Test
	void migrationConfigShouldWork() {

		var properties = new MigrationsProperties();
		properties.autocrlf = true;
		properties.database = Optional.of("db");
		properties.installedBy = Optional.of("ich");
		properties.impersonatedUser = Optional.of("hg");
		properties.schemaDatabase = Optional.of("db2");
		properties.transactionMode = MigrationsConfig.TransactionMode.PER_STATEMENT;
		properties.validateOnMigrate = false;
		properties.externalLocations = Optional.of(List.of("file:///bazbar", "dropped"));
		properties.delayBetweenMigrations = Optional.empty();

		var buildTimeProperties = new MigrationsBuildTimeProperties();
		buildTimeProperties.packagesToScan = Optional.of(List.of("foo"));
		buildTimeProperties.locationsToScan = List.of("bar");

		var config = new MigrationsRecorder().recordConfig(buildTimeProperties, properties, null, null).getValue();

		assertThat(config.getPackagesToScan()).containsExactlyElementsOf(buildTimeProperties.packagesToScan.get());
		assertThat(config.isAutocrlf()).isTrue();
		assertThat(config.getOptionalDatabase()).hasValue("db");
		assertThat(config.getOptionalInstalledBy()).hasValue("ich");
		assertThat(config.getOptionalImpersonatedUser()).hasValue("hg");
		assertThat(config.getLocationsToScan()).containsExactlyInAnyOrder("file:///bazbar", "bar");
		assertThat(config.getOptionalSchemaDatabase()).hasValue("db2");
		assertThat(config.getTransactionMode()).isEqualTo(MigrationsConfig.TransactionMode.PER_STATEMENT);
		assertThat(config.isValidateOnMigrate()).isFalse();
		assertThat(config.getOptionalDelayBetweenMigrations()).isEmpty();

		assertThat(config.getMigrationClassesDiscoverer()).isNotNull();
		assertThat(config.getResourceScanner()).isNotNull();
	}

	@Test
	void delayShallBeConfigurable() {

		var properties = new MigrationsProperties();
		properties.autocrlf = true;
		properties.database = Optional.empty();
		properties.installedBy = Optional.empty();
		properties.impersonatedUser = Optional.empty();
		properties.schemaDatabase = Optional.empty();
		properties.transactionMode = MigrationsConfig.TransactionMode.PER_STATEMENT;
		properties.externalLocations = Optional.empty();
		properties.delayBetweenMigrations = Optional.of(Duration.ofSeconds(1));

		var buildTimeProperties = new MigrationsBuildTimeProperties();
		buildTimeProperties.packagesToScan = Optional.empty();
		buildTimeProperties.locationsToScan = List.of("bar");

		var config = new MigrationsRecorder().recordConfig(buildTimeProperties, properties, null, null).getValue();
		assertThat(config.getOptionalDelayBetweenMigrations()).hasValue(Duration.ofSeconds(1));
	}
}
