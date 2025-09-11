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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import ac.simons.neo4j.migrations.core.Discoverer;
import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings({"squid:S2187"}) // Sonar doesn't realize that there are tests
class MigrationsAutoConfigurationTest {

	private static final Driver MOCKED_DRIVER = Mockito.mock(Driver.class);

	static {
		Mockito.doThrow(ServiceUnavailableException.class)
			.when(MOCKED_DRIVER).verifyConnectivity();
	}

	private static final MigrationsConfig DEFAULT_CONFIG = MigrationsConfig.defaultConfig();
	private static final Migrations DEFAULT_MIGRATIONS =
		new Migrations(DEFAULT_CONFIG, MOCKED_DRIVER);
	private static final MigrationsInitializer DEFAULT_INITIALIZER = new MigrationsInitializer(DEFAULT_MIGRATIONS);

	@Configuration(proxyBeanMethods = false)
	static class WithDriver {

		@Bean
		Driver driver() {
			return MOCKED_DRIVER;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class WithMigrationsConfig {

		@Bean
		MigrationsConfig migrationsConfig() {
			return DEFAULT_CONFIG;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class WithMigrations {

		@Bean
		Migrations migrations() {
			return DEFAULT_MIGRATIONS;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class WithInitializer {

		@Bean
		MigrationsInitializer migrationsInitializer() {
			return DEFAULT_INITIALIZER;
		}
	}

	static ObjectProvider<Discoverer<JavaBasedMigration>> noSpringDiscoverer() {
		return new StaticObjectProvider<>(null);
	}

	static ObjectProvider<ConfigBuilderCustomizer> noCustomizer() {
		return withCustomizer(null);
	}

	static ObjectProvider<ConfigBuilderCustomizer> withCustomizer(ConfigBuilderCustomizer customizer) {
		return new StaticObjectProvider<>(customizer);
	}

	private static class StaticObjectProvider<T> implements ObjectProvider<T> {
		private final T value;

		StaticObjectProvider(T value) {
			this.value = value;
		}

		@Override
		public T getObject(Object... args) throws BeansException {
			return value;
		}

		@Override
		public T getIfAvailable() throws BeansException {
			return value;
		}

		@Override
		public T getIfUnique() throws BeansException {
			return value;
		}

		@Override
		public T getObject() throws BeansException {
			if (value == null) {
				throw new BeanCreationException("No such bean");
			}
			return value;
		}

		@Override
		public Stream<T> orderedStream() {
			return value == null ? Stream.empty() : Stream.of(value);
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class LocationCheck {

		@Mock
		private ResourceLoader resourceLoader;

		@Mock
		private Resource resource;

		@Test
		void shouldCheckEmptyConfigFirst() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setLocationsToScan(new String[0]);
			properties.setPackagesToScan(new String[0]);

			assertThat(properties.isCheckLocation()).isTrue();

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), null))
				.withMessage("Neither locations nor packages to scan are configured.");
		}

		@Test
		void shouldWarnIfCheckLocationsCannotBeFoundAndPackagesAreEmpty() {

			LoggingAppender logAppender = new LoggingAppender();
			Logger logger = (Logger) LoggerFactory.getLogger(MigrationsAutoConfiguration.class);
			logger.addAppender(logAppender);
			logAppender.start();

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[0]);

			assertThat(properties.isCheckLocation()).isTrue();
			assertThat(properties.getLocationsToScan()).isNotEmpty();

			Mockito.when(resourceLoader.getResource(properties.getLocationsToScan()[0])).thenReturn(resource);
			Mockito.when(resource.exists()).thenReturn(false);

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			ac.neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());

			logAppender.stop();
			assertThat(logAppender.containsMessage(MigrationsAutoConfiguration.class.getName(),
					"No package to scan is configured and none of the configured locations exists."))
					.isTrue();

		}

		@Test
		void shouldBeHappyWithAtLeastOneLocation() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[0]);
			properties.setLocationsToScan(new String[] { "file:a", "file:b" });

			assertThat(properties.isCheckLocation()).isTrue();

			Mockito.when(resourceLoader.getResource(properties.getLocationsToScan()[0])).thenReturn(Mockito.mock(Resource.class));
			Mockito.when(resourceLoader.getResource(properties.getLocationsToScan()[1])).thenReturn(resource);
			Mockito.when(resource.exists()).thenReturn(true);

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			assertThatNoException().isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer()));
		}

		@Test
		void shouldBeHappyWithAtLeastOnePackage() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			assertThat(properties.isCheckLocation()).isTrue();

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			assertThatNoException().isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer()));
		}

		@Test // GH-237
		void validateOnMigrationShouldBeTrueByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isValidateOnMigrate()).isTrue();
		}

		@Test // GH-237
		void shouldApplyValidateOnMigration() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setValidateOnMigrate(false);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isValidateOnMigrate()).isFalse();
		}

		@Test // GH-238
		void autocrlfShouldBeFalseByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isAutocrlf()).isFalse();
		}

		@Test // GH-238
		void shouldApplyAutocrlf() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setAutocrlf(true);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isAutocrlf()).isTrue();
		}

		@Test
		void delayShouldBeNullByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getOptionalDelayBetweenMigrations()).isEmpty();
		}

		@Test
		void delayShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setDelayBetweenMigrations(Duration.ofMillis(667));

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getOptionalDelayBetweenMigrations()).hasValue(Duration.ofMillis(667));
		}

		@Test // GH-237
		void versionSortOrderShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setVersionSortOrder(MigrationsConfig.VersionSortOrder.SEMANTIC);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getVersionSortOrder()).isEqualTo(MigrationsConfig.VersionSortOrder.SEMANTIC);
		}

		@Test
		void cypherVersionShouldHaveDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.DATABASE_DEFAULT);
		}

		@Test
		void cypherVersionShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setCypherVersion(MigrationsConfig.CypherVersion.CYPHER_25);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.CYPHER_25);
		}

		@Test
		void shouldConfigureImpersonatedUser() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setImpersonatedUser("someoneElse");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getOptionalImpersonatedUser()).hasValue("someoneElse");
		}

		@Test
		void shouldConfigureSchemaDatabaseUser() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setSchemaDatabase("anotherDatabase");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getOptionalSchemaDatabase()).hasValue("anotherDatabase");
		}

		@Test
		void customizerShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setSchemaDatabase("anotherDatabase");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, withCustomizer(configBuilder -> configBuilder
				.withImpersonatedUser("blah")
				.withConstraintRenderingOptions(List.of(new RenderConfig.CypherRenderingOptions() {
					@Override
					public boolean useExplicitPropertyIndexType() {
						return true;
					}
				}))), noSpringDiscoverer());

			assertThat(config.getOptionalSchemaDatabase()).hasValue("anotherDatabase");
			assertThat(config.getOptionalImpersonatedUser()).hasValue("blah");
			assertThat(config.getConstraintRenderingOptions()).hasSize(1);
		}

		@Test // GH-1213
		void outOfOrderShouldBeDisallowedByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isOutOfOrder()).isFalse();
		}

		@Test // GH-1213
		void outOfOrderShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setOutOfOrder(true);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isOutOfOrder()).isTrue();
		}

		@Test
		void useFlywayCompatibleChecksumsShouldBeDisabled() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isUseFlywayCompatibleChecksums()).isFalse();
		}

		@Test
		void useFlywayCompatibleChecksumsShouldBeEnabled() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setUseFlywayCompatibleChecksums(true);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.isUseFlywayCompatibleChecksums()).isTrue();
		}

		@Test // GH-1536
		void targetShouldBeNullByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getTarget()).isNull();
		}

		@Test // GH-1536
		void targetShouldBeApplied() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setTarget("0.10.0");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties, noCustomizer(), noSpringDiscoverer());
			assertThat(config.getTarget()).isEqualTo("0.10.0");
		}
	}

	private static class LoggingAppender extends ListAppender<ILoggingEvent> {

		Map<String, List<String>> logMessages = new HashMap<>();

		LoggingAppender() {
			setContext((Context) LoggerFactory.getILoggerFactory());
		}

		@Override
		protected void append(ILoggingEvent logEvent) {
			String loggerName = logEvent.getLoggerName();
			String message = logEvent.getMessage();

			logMessages.putIfAbsent(loggerName,	new ArrayList<>());
			logMessages.get(loggerName).add(message);
		}

		public boolean containsMessage(String loggerName, String message) {
			if (logMessages.get(loggerName) == null) {
				return false;
			}
			return logMessages.get(loggerName).contains(message);
		}
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MigrationsAutoConfiguration.class));

	@Nested
	@ExtendWith(MockitoExtension.class)
	class TopLevelConditions {

		@Test
		void mustNotFailIfTheDriverStarterIsNotPresent() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withClassLoader(new FilteredClassLoader(Neo4jAutoConfiguration.class))
				.run(ctx -> assertThat(ctx).hasSingleBean(Migrations.class));
		}

		@Test
		void shouldRequireMigrations() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withClassLoader(new FilteredClassLoader(Migrations.class))
				.run(ctx -> assertThat(ctx).doesNotHaveBean(Migrations.class));
		}

		@Test
		void shouldRequireDriverBean() {

			contextRunner
				.run(ctx -> assertThat(ctx).doesNotHaveBean(Migrations.class));
		}

		@Test
		void shouldRespectEnabledProperty() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withPropertyValues("org.neo4j.migrations.enabled=false")
				.run(ctx -> assertThat(ctx)
					.hasSingleBean(Migrations.class)
					.doesNotHaveBean(MigrationsInitializer.class)
				);
		}
	}

	@Nested
	class MethodLevelConditions {

		@Test
		void shouldCreateMigrationsConfig() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.run(ctx ->
					assertThat(ctx).hasSingleBean(MigrationsConfig.class)
				);
		}

		@Test
		void shouldBackOffForCustomMigrationsConfig() {

			contextRunner
				.withUserConfiguration(WithDriver.class, WithMigrationsConfig.class)
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(MigrationsConfig.class);
					assertThat(ctx.getBean(MigrationsConfig.class)).isEqualTo(DEFAULT_CONFIG);
				});
		}

		@Test
		void shouldCreateMigrations() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.run(ctx ->
					assertThat(ctx).hasSingleBean(Migrations.class)
				);
		}

		@Test
		void shouldBackOffForCustomMigrations() {

			contextRunner
				.withUserConfiguration(WithDriver.class, WithMigrations.class)
				.run(ctx -> {
					assertThat(ctx).doesNotHaveBean(MigrationsConfig.class);
					assertThat(ctx).hasSingleBean(Migrations.class);
					assertThat(ctx.getBean(Migrations.class)).isEqualTo(DEFAULT_MIGRATIONS);
				});
		}

		@Test
		void shouldCreateInitializer() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.run(ctx ->
					assertThat(ctx).hasSingleBean(MigrationsInitializer.class)
				);
		}

		@Test
		void shouldBackOffForCustomInitializer() {
			contextRunner
				.withUserConfiguration(WithDriver.class, WithInitializer.class)
				.run(ctx -> {
					assertThat(ctx).doesNotHaveBean(MigrationsConfig.class);
					assertThat(ctx).doesNotHaveBean(Migrations.class);
					assertThat(ctx).hasSingleBean(MigrationsInitializer.class);
					assertThat(ctx.getBean(MigrationsInitializer.class)).isEqualTo(DEFAULT_INITIALIZER);
				});
		}
	}

	@Nested
	class ConfigurationCreation {

		@Test
		void shouldFailOnInvalidLocations() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withPropertyValues("org.neo4j.migrations.locations-to-scan=")
				.run(ctx -> {
					assertThat(ctx).hasFailed();
					assertThat(ctx).getFailure().isInstanceOf(BeanCreationException.class);
					assertThat(ctx).getFailure()
						.hasMessageContaining("Neither locations nor packages to scan are configured.");
				});
		}

		@Test
		void shouldBeLenientIfConfiguredToBe() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withPropertyValues(
					"org.neo4j.migrations.locations-to-scan=",
					"org.neo4j.migrations.check-location=false")
				.run(ctx ->
					assertThat(ctx).hasSingleBean(Migrations.class));
		}

		@Test
		void shouldCreateCorrectConfiguration() {

			contextRunner
				.withUserConfiguration(WithDriver.class)
				.withPropertyValues(
					"org.neo4j.migrations.locations-to-scan=classpath:i/dont/care,file:/neither/do/i",
					"org.neo4j.migrations.packages-to-scan=i.dont.exists,me.neither",
					"org.neo4j.migrations.transaction-mode=PER_STATEMENT",
					"org.neo4j.migrations.database=anAwesomeDatabase",
					"org.neo4j.migrations.installed-by=James Bond",
					"org.neo4j.migrations.check-location=false")
				.run(ctx -> {

					assertThat(ctx).hasSingleBean(MigrationsConfig.class);
					MigrationsConfig config = ctx.getBean(MigrationsConfig.class);
					assertThat(config.getLocationsToScan())
						.containsExactly("classpath:i/dont/care", "file:/neither/do/i");
					assertThat(config.getPackagesToScan()).containsExactly("i.dont.exists", "me.neither");
					assertThat(config.getTransactionMode()).isEqualTo(TransactionMode.PER_STATEMENT);
					assertThat(config.getOptionalInstalledBy()).hasValue("James Bond");
				});
		}
	}
}
