/*
 * Copyright 2020 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsException;

import java.util.Arrays;

import org.neo4j.driver.Driver;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Neo4j migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.6
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Migrations.class)
@ConditionalOnBean(Driver.class)
@ConditionalOnProperty(prefix = "org.neo4j.migrations", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({ Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class })
@EnableConfigurationProperties({ MigrationsProperties.class })
public class MigrationsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ MigrationsConfig.class, Migrations.class, MigrationsInitializer.class })
	MigrationsConfig neo4jMigrationsConfig(ResourceLoader resourceLoader, MigrationsProperties migrationsProperties) {

		if (migrationsProperties.isCheckLocation()) {
			checkLocationExists(resourceLoader, migrationsProperties);
		}

		return MigrationsConfig.builder()
			.withLocationsToScan(migrationsProperties.getLocationsToScan())
			.withPackagesToScan(migrationsProperties.getPackagesToScan())
			.withTransactionMode(migrationsProperties.getTransactionMode())
			.withDatabase(migrationsProperties.getDatabase())
			.withInstalledBy(migrationsProperties.getInstalledBy())
			.build();
	}

	@Bean
	@ConditionalOnMissingBean({ Migrations.class, MigrationsInitializer.class })
	Migrations neo4jMigrations(MigrationsConfig neo4jMigrationsConfig, Driver driver) {

		return new Migrations(neo4jMigrationsConfig, driver);
	}

	@Bean
	@ConditionalOnMissingBean({ MigrationsInitializer.class })
	MigrationsInitializer neo4jMigrationsInitializer(Migrations neo4jMigrations) {

		return new MigrationsInitializer(neo4jMigrations);
	}

	private static void checkLocationExists(ResourceLoader resourceLoader, MigrationsProperties properties) {

		if (properties.getLocationsToScan().length == 0 && properties.getPackagesToScan().length == 0) {
			throw new MigrationsException("Neither locations nor packages to scan are configured.");
		}

		if (properties.getPackagesToScan().length == 0 &&
			!hasAtLeastOneLocation(resourceLoader, properties.getLocationsToScan())) {

			throw new MigrationsException(
				"No package to scan is configured and none of the configured locations exists.");
		}
	}

	private static boolean hasAtLeastOneLocation(ResourceLoader resourceLoader, String[] locations) {

		return Arrays.stream(locations)
			.map(resourceLoader::getResource)
			.anyMatch(Resource::exists);
	}
}
