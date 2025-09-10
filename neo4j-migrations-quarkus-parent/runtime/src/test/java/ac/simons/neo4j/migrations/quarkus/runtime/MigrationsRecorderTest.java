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
package ac.simons.neo4j.migrations.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.MigrationsConfig;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrationsRecorderTest {

	@Test
	void migrationConfigShouldWork() {

		var properties = mock(MigrationsProperties.class);
		when(properties.autocrlf()).thenReturn(true);
		when(properties.database()).thenReturn(Optional.of("db"));
		when(properties.installedBy()).thenReturn(Optional.of("ich"));
		when(properties.impersonatedUser()).thenReturn(Optional.of("hg"));
		when(properties.schemaDatabase()).thenReturn(Optional.of("db2"));
		when(properties.transactionMode()).thenReturn(MigrationsConfig.TransactionMode.PER_STATEMENT);
		when(properties.validateOnMigrate()).thenReturn(false);
		when(properties.externalLocations()).thenReturn(Optional.of(List.of("file:///bazbar", "dropped")));
		when(properties.delayBetweenMigrations()).thenReturn(Optional.empty());

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.of(List.of("foo")));
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();

		assertThat(config.getPackagesToScan()).containsExactlyElementsOf(buildTimeProperties.packagesToScan().orElse(List.of()));
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

		var properties = mock(MigrationsProperties.class);
		when(properties.autocrlf()).thenReturn(true);
		when(properties.database()).thenReturn(Optional.empty());
		when(properties.installedBy()).thenReturn(Optional.empty());
		when(properties.impersonatedUser()).thenReturn(Optional.empty());
		when(properties.schemaDatabase()).thenReturn(Optional.empty());
		when(properties.transactionMode()).thenReturn(MigrationsConfig.TransactionMode.PER_STATEMENT);
		when(properties.externalLocations()).thenReturn(Optional.empty());
		when(properties.delayBetweenMigrations()).thenReturn(Optional.of(Duration.ofSeconds(1)));

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.getOptionalDelayBetweenMigrations()).hasValue(Duration.ofSeconds(1));
	}

	@Test // GH-1213
	void outOfOrderShouldBeDisallowedByDefault() {

		var properties = mock(MigrationsProperties.class);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.isOutOfOrder()).isFalse();
	}

	@Test // GH-1213
	void outOfOrderShouldBeApplied() {

		var properties = mock(MigrationsProperties.class);
		when(properties.outOfOrder()).thenReturn(true);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.isOutOfOrder()).isTrue();
	}

	@Test
	void useFlywayCompatibleChecksumsShouldBeDisabled() {

		var properties = mock(MigrationsProperties.class);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.isUseFlywayCompatibleChecksums()).isFalse();
	}

	@Test
	void useFlywayCompatibleChecksumsShouldBeEnabled() {

		var properties = mock(MigrationsProperties.class);
		when(properties.useFlywayCompatibleChecksums()).thenReturn(true);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.isUseFlywayCompatibleChecksums()).isTrue();
	}

	@Test // GH-1536
	void targetShouldBeNullByDefault() {

		var properties = mock(MigrationsProperties.class);
		when(properties.target()).thenReturn(Optional.empty());

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.getTarget()).isNull();
	}

	@Test // GH-1536
	void targetShouldBeApplied() {

		var properties = mock(MigrationsProperties.class);
		when(properties.target()).thenReturn(Optional.of("latest"));

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		when(buildTimeProperties.packagesToScan()).thenReturn(Optional.empty());
		when(buildTimeProperties.locationsToScan()).thenReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties)).recordConfig(buildTimeProperties, null, null).getValue();
		assertThat(config.getTarget()).isEqualTo("latest");
	}
}
