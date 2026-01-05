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
package ac.simons.neo4j.migrations.quarkus.runtime;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import ac.simons.neo4j.migrations.core.MigrationsConfig;
import io.quarkus.runtime.RuntimeValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationsRecorderTests {

	static {
		System.setProperty("org.jboss.logging.provider", "jdk");
	}

	private final LogCapturingHandler handler;

	MigrationsRecorderTests() {
		this.handler = new LogCapturingHandler();
	}

	@BeforeAll
	void setup() {

		var jdkLogger = java.util.logging.Logger.getLogger("ac.simons.neo4j.migrations.quarkus.runtime");
		jdkLogger.addHandler(this.handler);
	}

	@AfterAll
	void cleanup() {
		var jdkLogger = java.util.logging.Logger.getLogger("ac.simons.neo4j.migrations.quarkus.runtime");
		jdkLogger.removeHandler(this.handler);
	}

	@Test
	void migrationConfigShouldWork() {

		var properties = mock(MigrationsProperties.class);
		given(properties.autocrlf()).willReturn(true);
		given(properties.database()).willReturn(Optional.of("db"));
		given(properties.installedBy()).willReturn(Optional.of("ich"));
		given(properties.impersonatedUser()).willReturn(Optional.of("hg"));
		given(properties.schemaDatabase()).willReturn(Optional.of("db2"));
		given(properties.transactionMode()).willReturn(MigrationsConfig.TransactionMode.PER_STATEMENT);
		given(properties.validateOnMigrate()).willReturn(false);
		given(properties.externalLocations()).willReturn(Optional.of(List.of("file:///bazbar", "dropped")));
		given(properties.delayBetweenMigrations()).willReturn(Optional.empty());

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.of(List.of("foo")));
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();

		assertThat(config.getPackagesToScan())
			.containsExactlyElementsOf(buildTimeProperties.packagesToScan().orElse(List.of()));
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
		assertThat(this.handler.messages)
			.containsExactly("External locations only support filesystem locations, ignoring `dropped`");
	}

	@Test
	void delayShallBeConfigurable() {

		var properties = mock(MigrationsProperties.class);
		given(properties.autocrlf()).willReturn(true);
		given(properties.database()).willReturn(Optional.empty());
		given(properties.installedBy()).willReturn(Optional.empty());
		given(properties.impersonatedUser()).willReturn(Optional.empty());
		given(properties.schemaDatabase()).willReturn(Optional.empty());
		given(properties.transactionMode()).willReturn(MigrationsConfig.TransactionMode.PER_STATEMENT);
		given(properties.externalLocations()).willReturn(Optional.empty());
		given(properties.delayBetweenMigrations()).willReturn(Optional.of(Duration.ofSeconds(1)));

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.getOptionalDelayBetweenMigrations()).hasValue(Duration.ofSeconds(1));
	}

	@Test // GH-1213
	void outOfOrderShouldBeDisallowedByDefault() {

		var properties = mock(MigrationsProperties.class);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.isOutOfOrder()).isFalse();
	}

	@Test // GH-1213
	void outOfOrderShouldBeApplied() {

		var properties = mock(MigrationsProperties.class);
		given(properties.outOfOrder()).willReturn(true);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.isOutOfOrder()).isTrue();
	}

	@Test
	void useFlywayCompatibleChecksumsShouldBeDisabled() {

		var properties = mock(MigrationsProperties.class);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.isUseFlywayCompatibleChecksums()).isFalse();
	}

	@Test
	void useFlywayCompatibleChecksumsShouldBeEnabled() {

		var properties = mock(MigrationsProperties.class);
		given(properties.useFlywayCompatibleChecksums()).willReturn(true);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.isUseFlywayCompatibleChecksums()).isTrue();
	}

	@Test // GH-1536
	void targetShouldBeNullByDefault() {

		var properties = mock(MigrationsProperties.class);
		given(properties.target()).willReturn(Optional.empty());

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.getTarget()).isNull();
	}

	@Test // GH-1536
	void targetShouldBeApplied() {

		var properties = mock(MigrationsProperties.class);
		given(properties.target()).willReturn(Optional.of("latest"));

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.getTarget()).isEqualTo("latest");
	}

	@Test
	void cypherVersionShouldHaveDefault() {

		var properties = mock(MigrationsProperties.class);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.DATABASE_DEFAULT);
	}

	@Test
	void cypherVersionShouldBeApplied() {

		var properties = mock(MigrationsProperties.class);
		given(properties.cypherVersion()).willReturn(MigrationsConfig.CypherVersion.CYPHER_25);

		var buildTimeProperties = mock(MigrationsBuildTimeProperties.class);
		given(buildTimeProperties.packagesToScan()).willReturn(Optional.empty());
		given(buildTimeProperties.locationsToScan()).willReturn(List.of("bar"));

		var config = new MigrationsRecorder(new RuntimeValue<>(properties))
			.recordConfig(buildTimeProperties, null, null)
			.getValue();
		assertThat(config.getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.CYPHER_25);
	}

	static class LogCapturingHandler extends Handler {

		final List<String> messages = new ArrayList<>();

		LogCapturingHandler() {
			this.setLevel(Level.ALL);
		}

		@Override
		public void publish(LogRecord logRecord) {
			if (logRecord.getLevel().intValue() < this.getLevel().intValue()) {
				return;
			}
			this.messages.add(MessageFormat.format(logRecord.getMessage(), logRecord.getParameters()));
		}

		@Override
		public void flush() {
			// Nothing to be flushed
		}

		@Override
		public void close() {
			// Nothing to be closed
		}

	}

}
