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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.BeanCreationException;
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
				.isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties))
				.withMessage("Neither locations nor packages to scan are configured.");
		}

		@Test
		void shouldCheckLocationsIfPackagesAreEmpty() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[0]);

			assertThat(properties.isCheckLocation()).isTrue();
			assertThat(properties.getLocationsToScan()).isNotEmpty();

			Mockito.when(resourceLoader.getResource(properties.getLocationsToScan()[0])).thenReturn(resource);
			Mockito.when(resource.exists()).thenReturn(false);

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties))
				.withMessage("No package to scan is configured and none of the configured locations exists.");
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
			assertThatNoException().isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties));
		}

		@Test
		void shouldBeHappyWithAtLeastOnePackage() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			assertThat(properties.isCheckLocation()).isTrue();

			MigrationsAutoConfiguration ac = new MigrationsAutoConfiguration();
			assertThatNoException().isThrownBy(() -> ac.neo4jMigrationsConfig(resourceLoader, properties));
		}

		@Test // GH-237
		void validateOnMigrationShouldBeTrueByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.isValidateOnMigrate()).isTrue();
		}

		@Test // GH-237
		void shouldApplyValidateOnMigration() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setValidateOnMigrate(false);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.isValidateOnMigrate()).isFalse();
		}

		@Test // GH-238
		void autocrlfShouldBeFalseByDefault() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.isAutocrlf()).isFalse();
		}

		@Test // GH-238
		void shouldApplyAutocrlf() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setAutocrlf(true);

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.isAutocrlf()).isTrue();
		}

		@Test
		void shouldConfigureImpersonatedUser() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setImpersonatedUser("someoneElse");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.getOptionalImpersonatedUser()).hasValue("someoneElse");
		}

		@Test
		void shouldConfigureSchemaDatabaseUser() {

			MigrationsProperties properties = new MigrationsProperties();
			properties.setPackagesToScan(new String[] { "na" });
			properties.setSchemaDatabase("anotherDatabase");

			MigrationsConfig config = new MigrationsAutoConfiguration().neo4jMigrationsConfig(resourceLoader, properties);
			assertThat(config.getOptionalSchemaDatabase()).hasValue("anotherDatabase");
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
				.run(ctx -> assertThat(ctx).doesNotHaveBean(Migrations.class));
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
