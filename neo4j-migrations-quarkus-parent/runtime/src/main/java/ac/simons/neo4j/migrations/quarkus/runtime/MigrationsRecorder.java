/*
 * Copyright 2020-2022 the original author or authors.
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

import ac.simons.neo4j.migrations.core.ClasspathResourceScanner;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.internal.Location;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.Driver;

/**
 * Records both initialization of the {@link MigrationsConfig migration config} and the {@link Migrations migrations} itself.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@Recorder
public class MigrationsRecorder {

	/**
	 * Records the configuration
	 *
	 * @param runtimeProperties   runtime properties
	 * @param buildTimeProperties build-time properties
	 * @param discoverer          the (static) discovered configured during build time
	 * @param resourceScanner     the (static) scanner configured during build time
	 * @return A runtime value containing the configuration
	 */
	public RuntimeValue<MigrationsConfig> recordConfig(
		MigrationsBuildTimeProperties buildTimeProperties,
		MigrationsProperties runtimeProperties,
		StaticJavaBasedMigrationDiscoverer discoverer, ClasspathResourceScanner resourceScanner
	) {

		var allLocationsToScan = new ArrayList<String>(
			buildTimeProperties.locationsToScan.size() + runtimeProperties.externalLocations.map(
				List::size).orElse(0));
		allLocationsToScan.addAll(buildTimeProperties.locationsToScan);
		runtimeProperties.externalLocations.ifPresent(locations -> locations.stream()
			.filter(l -> Location.of(l).getType() == Location.LocationType.FILESYSTEM)
			.forEach(allLocationsToScan::add));

		var config = MigrationsConfig.builder()
			.withLocationsToScan(allLocationsToScan.toArray(new String[0]))
			.withPackagesToScan(buildTimeProperties.packagesToScan.map(v -> v.toArray(String[]::new)).orElse(null))
			.withTransactionMode(runtimeProperties.transactionMode)
			.withDatabase(runtimeProperties.database.orElse(null))
			.withSchemaDatabase(runtimeProperties.schemaDatabase.orElse(null))
			.withImpersonatedUser(runtimeProperties.impersonatedUser.orElse(null))
			.withInstalledBy(runtimeProperties.installedBy.orElse(null))
			.withValidateOnMigrate(runtimeProperties.validateOnMigrate)
			.withAutocrlf(runtimeProperties.autocrlf)
			.withMigrationClassesDiscoverer(discoverer)
			.withResourceScanner(resourceScanner)
			.build();

		return new RuntimeValue<>(config);
	}

	/**
	 * Records the enabled-flag
	 *
	 * @param runtimeProperties runtime properties
	 * @return A runtime value containing the enabled-flag
	 */
	public RuntimeValue<MigrationsEnabled> recordIsEnabled(MigrationsProperties runtimeProperties) {
		return new RuntimeValue<>(new MigrationsEnabled(runtimeProperties.enabled));
	}

	/**
	 * Records the migration itself.
	 *
	 * @param migrationsConfig The runtime value for the configuration
	 * @param driver           The runtime value of a driver instance
	 * @return A runtime value containing the migrations
	 */
	public RuntimeValue<Migrations> recordMigrations(
		RuntimeValue<MigrationsConfig> migrationsConfig, RuntimeValue<Driver> driver
	) {
		return new RuntimeValue<>(new Migrations(migrationsConfig.getValue(), driver.getValue()));
	}
}
