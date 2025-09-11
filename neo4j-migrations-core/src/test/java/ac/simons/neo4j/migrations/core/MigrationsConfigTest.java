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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Michael J. Simons
 */
class MigrationsConfigTest {

	@Test
	void shouldConfigureDefaultClasspathPackage() {

		assertThat(MigrationsConfig.builder().build().getLocationsToScan()).containsExactlyElementsOf(
			Defaults.LOCATIONS_TO_SCAN);
	}

	@Test // GH-237
	void validateOnInstallShouldBeTrueByDefault() {

		assertThat(MigrationsConfig.builder().build().isValidateOnMigrate()).isTrue();
	}

	@Test // GH-237
	void validateOnInstallShouldBeChangeable() {

		assertThat(MigrationsConfig.builder().withValidateOnMigrate(false).build().isValidateOnMigrate()).isFalse();
	}

	@Test // GH-238
	void autocrlfShouldBeFalseByDefault() {

		assertThat(MigrationsConfig.builder().build().isAutocrlf()).isFalse();
	}

	@Test // GH-238
	void autocrlfShouldBeChangeable() {

		assertThat(MigrationsConfig.builder().withAutocrlf(true).build().isAutocrlf()).isTrue();
	}

	@Test // GH-1213
	void outOfOrderShouldBeFalseByDefault() {

		assertThat(MigrationsConfig.builder().build().isOutOfOrder()).isFalse();
	}

	@Test // GH-1213
	void outOfOrderShouldBeChangeable() {

		assertThat(MigrationsConfig.builder().withOutOfOrderAllowed(true).build().isOutOfOrder()).isTrue();
	}

	@Test
	void logToShouldWork() {

		MigrationsConfig config = MigrationsConfig.builder().build();
		Logger logger = Logger.getLogger(UUID.randomUUID().toString());
		LogCollector logCollector = new LogCollector();
		logger.addHandler(logCollector);

		config.logTo(logger, true);
		assertThat(logCollector.logMessages)
			.containsExactly("Will search for Cypher scripts in \"classpath:neo4j/migrations\"",
				"Statements will be applied in one transaction per migration");
	}

	static Stream<Arguments> shouldConfigureCypherVersion() {
		return Stream.of(Arguments.of(null, Defaults.CYPHER_VERSION),
			Arguments.of(MigrationsConfig.CypherVersion.CYPHER_5, MigrationsConfig.CypherVersion.CYPHER_5),
			Arguments.of(MigrationsConfig.CypherVersion.CYPHER_25, MigrationsConfig.CypherVersion.CYPHER_25)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldConfigureCypherVersion(MigrationsConfig.CypherVersion in, MigrationsConfig.CypherVersion expected) {

		var config = MigrationsConfig.builder().withCypherVersion(in).build();
		assertThat(config.getCypherVersion()).isEqualTo(expected);
	}

	@Test
	void shouldDefaultToNoCypherVersion() {

		var config = MigrationsConfig.builder().build();
		assertThat(config.getCypherVersion()).isEqualTo(Defaults.CYPHER_VERSION);
	}

	@Test
	void shouldWarnLog() {

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan()
			.build();
		Logger logger = Logger.getLogger(UUID.randomUUID().toString());
		LogCollector logCollector = new LogCollector();
		logger.addHandler(logCollector);

		config.logTo(logger, true);
		assertThat(logCollector.logMessages)
			.containsExactly("Cannot find migrations as neither locations nor packages to scan are configured!");
	}

	@Test
	void shouldOnlyVerboseLogIfPossible() {

		MigrationsConfig config = MigrationsConfig.builder().build();

		Logger logger = Logger.getLogger(UUID.randomUUID().toString());
		LogCollector logCollector = new LogCollector();
		logger.addHandler(logCollector);

		config.logTo(logger, false);
		assertThat(logCollector.logMessages).isEmpty();

		logCollector.clear();

		logger.setLevel(Level.WARNING);
		config.logTo(logger, true);
		assertThat(logCollector.logMessages).isEmpty();
	}

	@Test
	void nonDefaultsShouldBeLogged() {

		MigrationsConfig config = MigrationsConfig
			.builder()
			.withPackagesToScan("a.b.c")
			.withDatabase("x")
			.withTransactionMode(MigrationsConfig.TransactionMode.PER_STATEMENT)
			.build();

		Logger logger = Logger.getLogger(UUID.randomUUID().toString());
		LogCollector logCollector = new LogCollector();
		logger.addHandler(logCollector);

		config.logTo(logger, true);
		assertThat(logCollector.logMessages)
			.containsExactly(
				"Migrations will be applied to database \"x\"",
				"Will search for Cypher scripts in \"classpath:neo4j/migrations\"",
				"Statements will be applied in separate transactions",
				"Will scan for Java-based migrations in \"a.b.c\""
			);
	}

	static class LogCollector extends Handler {

		final List<String> logMessages = new ArrayList<>();
		final SimpleFormatter formatter = new SimpleFormatter();

		void clear() {
			logMessages.clear();
		}

		@Override public void publish(LogRecord record) {
			logMessages.add(formatter.formatMessage(record));
		}

		@Override public void flush() {
		}

		@Override public void close() throws SecurityException {
		}
	}

	@Nested
	class SchemaDatabase {

		@Test
		void shouldDefaultToNull() {

			assertThat(MigrationsConfig.defaultConfig().getOptionalSchemaDatabase()).isEmpty();
		}

		@Test
		void shoulldBeConfigurableIndependendFromDatabase() {

			MigrationsConfig config = MigrationsConfig.builder()
				.withSchemaDatabase("sd")
				.withDatabase("d")
				.build();

			assertThat(config.getOptionalSchemaDatabase()).hasValue("sd");
			assertThat(config.getOptionalDatabase()).hasValue("d");
		}
	}
}
