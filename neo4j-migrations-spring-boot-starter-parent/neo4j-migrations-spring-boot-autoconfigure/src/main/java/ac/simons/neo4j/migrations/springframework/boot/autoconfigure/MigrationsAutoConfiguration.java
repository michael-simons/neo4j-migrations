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

import java.util.Arrays;

import ac.simons.neo4j.migrations.core.Discoverer;
import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Neo4j migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.6
 */
@AutoConfiguration
@ConditionalOnClass(Migrations.class)
@ConditionalOnBean(Driver.class)
@AutoConfigureAfter({ Neo4jAutoConfiguration.class })
@AutoConfigureBefore({ Neo4jDataAutoConfiguration.class })
@EnableConfigurationProperties({ MigrationsProperties.class })
public class MigrationsAutoConfiguration {

	private static final Log LOG = LogFactory.getLog(MigrationsAutoConfiguration.class);

	private static void checkLocationExists(ResourceLoader resourceLoader, MigrationsProperties properties) {

		if (properties.getLocationsToScan().length == 0 && properties.getPackagesToScan().length == 0) {
			throw new MigrationsException("Neither locations nor packages to scan are configured.");
		}

		if (properties.getPackagesToScan().length == 0
				&& !hasAtLeastOneLocation(resourceLoader, properties.getLocationsToScan())) {

			LOG.warn("No package to scan is configured and none of the configured locations exists.");
		}
	}

	private static boolean hasAtLeastOneLocation(ResourceLoader resourceLoader, String[] locations) {

		return Arrays.stream(locations).map(resourceLoader::getResource).anyMatch(Resource::exists);
	}

	@Bean
	@ConditionalOnMissingBean({ MigrationsConfig.class, Migrations.class, MigrationsInitializer.class })
	MigrationsConfig neo4jMigrationsConfig(ResourceLoader resourceLoader, MigrationsProperties migrationsProperties,
			ObjectProvider<ConfigBuilderCustomizer> configBuilderCustomizers,
			ObjectProvider<Discoverer<JavaBasedMigration>> applicationContextAwareDiscoverer) {

		if (migrationsProperties.isCheckLocation()) {
			checkLocationExists(resourceLoader, migrationsProperties);
		}

		var builder = MigrationsConfig.builder()
			.withLocationsToScan(migrationsProperties.getLocationsToScan())
			.withPackagesToScan(migrationsProperties.getPackagesToScan())
			.withTransactionMode(migrationsProperties.getTransactionMode())
			.withTransactionTimeout(migrationsProperties.getTransactionTimeout())
			.withDatabase(migrationsProperties.getDatabase())
			.withSchemaDatabase(migrationsProperties.getSchemaDatabase())
			.withImpersonatedUser(migrationsProperties.getImpersonatedUser())
			.withInstalledBy(migrationsProperties.getInstalledBy())
			.withValidateOnMigrate(migrationsProperties.isValidateOnMigrate())
			.withAutocrlf(migrationsProperties.isAutocrlf())
			.withDelayBetweenMigrations(migrationsProperties.getDelayBetweenMigrations())
			.withVersionSortOrder(migrationsProperties.getVersionSortOrder())
			.withMigrationClassesDiscoverer(applicationContextAwareDiscoverer.getIfAvailable())
			.withOutOfOrderAllowed(migrationsProperties.isOutOfOrder())
			.withFlywayCompatibleChecksums(migrationsProperties.isUseFlywayCompatibleChecksums())
			.withTarget(migrationsProperties.getTarget())
			.withCypherVersion(migrationsProperties.getCypherVersion());
		configBuilderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean({ Migrations.class, MigrationsInitializer.class })
	Migrations neo4jMigrations(MigrationsConfig neo4jMigrationsConfig, Driver driver) {

		return new Migrations(neo4jMigrationsConfig, driver);
	}

	@Bean
	@ConditionalOnMissingBean({ MigrationsInitializer.class })
	@ConditionalOnProperty(prefix = "org.neo4j.migrations", name = "enabled", matchIfMissing = true)
	MigrationsInitializer neo4jMigrationsInitializer(Migrations neo4jMigrations) {

		return new MigrationsInitializer(neo4jMigrations);
	}

	@Bean
	@ConditionalOnMissingBean({ Discoverer.class })
	@ConditionalOnProperty(prefix = "org.neo4j.migrations", name = "enabled", matchIfMissing = true)
	Discoverer<JavaBasedMigration> applicationContextAwareDiscoverer(
			ObjectProvider<JavaBasedMigration> javaBasedMigrations) {

		return new ApplicationContextAwareDiscoverer(javaBasedMigrations);
	}

}
