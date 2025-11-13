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

import java.util.ArrayList;
import java.util.List;

import ac.simons.neo4j.migrations.core.ClasspathResourceScanner;
import ac.simons.neo4j.migrations.core.Location;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

/**
 * Records both initialization of the {@link MigrationsConfig migration config} and the
 * {@link Migrations migrations} itself.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@Recorder
public class MigrationsRecorder {

	private static final Logger LOG = Logger.getLogger("ac.simons.neo4j.migrations.quarkus.runtime");

	private final RuntimeValue<MigrationsProperties> rtvruntimeProperties;

	public MigrationsRecorder(RuntimeValue<MigrationsProperties> rtvruntimeProperties) {
		this.rtvruntimeProperties = rtvruntimeProperties;
	}

	/**
	 * Records the configuration.
	 * @param buildTimeProperties build-time properties
	 * @param discoverer the (static) discovered configured during build time
	 * @param resourceScanner the (static) scanner configured during build time
	 * @return a runtime value containing the configuration
	 */
	public RuntimeValue<MigrationsConfig> recordConfig(MigrationsBuildTimeProperties buildTimeProperties,
			StaticJavaBasedMigrationDiscoverer discoverer, ClasspathResourceScanner resourceScanner) {

		var runtimeProperties = this.rtvruntimeProperties.getValue();
		var allLocationsToScan = new ArrayList<String>(buildTimeProperties.locationsToScan().size()
				+ runtimeProperties.externalLocations().map(List::size).orElse(0));
		allLocationsToScan.addAll(buildTimeProperties.locationsToScan());
		runtimeProperties.externalLocations().ifPresent(locations -> locations.stream().filter(l -> {
			var isFileSystem = Location.of(l).getType() == Location.LocationType.FILESYSTEM;
			if (!isFileSystem) {
				LOG.warnv("External locations only support filesystem locations, ignoring `{0}`", l);
			}
			return isFileSystem;
		}).forEach(allLocationsToScan::add));

		var config = MigrationsConfig.builder()
			.withLocationsToScan(allLocationsToScan.toArray(new String[0]))
			.withPackagesToScan(buildTimeProperties.packagesToScan().map(v -> v.toArray(String[]::new)).orElse(null))
			.withTransactionMode(runtimeProperties.transactionMode())
			.withTransactionTimeout(runtimeProperties.transactionTimeout().orElse(null))
			.withDatabase(runtimeProperties.database().orElse(null))
			.withSchemaDatabase(runtimeProperties.schemaDatabase().orElse(null))
			.withImpersonatedUser(runtimeProperties.impersonatedUser().orElse(null))
			.withInstalledBy(runtimeProperties.installedBy().orElse(null))
			.withValidateOnMigrate(runtimeProperties.validateOnMigrate())
			.withAutocrlf(runtimeProperties.autocrlf())
			.withMigrationClassesDiscoverer(discoverer)
			.withResourceScanner(resourceScanner)
			.withDelayBetweenMigrations(runtimeProperties.delayBetweenMigrations().orElse(null))
			.withVersionSortOrder(runtimeProperties.versionSortOrder())
			.withOutOfOrderAllowed(runtimeProperties.outOfOrder())
			.withFlywayCompatibleChecksums(runtimeProperties.useFlywayCompatibleChecksums())
			.withTarget(runtimeProperties.target().orElse(null))
			.withCypherVersion(runtimeProperties.cypherVersion())
			.build();

		return new RuntimeValue<>(config);
	}

	/**
	 * Records the enabled-flag. The actual value of the property <strong>must</strong> be
	 * recorded here as well before use.
	 * @return a runtime value containing the enabled-flag
	 */
	public RuntimeValue<Boolean> isEnabled() {
		return new RuntimeValue<>(this.rtvruntimeProperties.getValue().enabled());
	}

	/**
	 * Records the migration itself.
	 * @param migrationsConfig the runtime value for the configuration
	 * @param driver the runtime value of a driver instance
	 * @return a runtime value containing the migrations
	 */
	public RuntimeValue<Migrations> recordMigrations(RuntimeValue<MigrationsConfig> migrationsConfig,
			RuntimeValue<Driver> driver) {
		return new RuntimeValue<>(new Migrations(migrationsConfig.getValue(), driver.getValue()));
	}

	/**
	 * Applies the migrations if {@link MigrationsProperties#enabled} is {@literal true}.
	 * @param migrationsRv the runtime value containing the migrations instance
	 * @param migrationsEnabledRv the runtime value containing the flag whether migrations
	 * are enabled or not
	 */
	public void applyMigrations(RuntimeValue<Migrations> migrationsRv, RuntimeValue<Boolean> migrationsEnabledRv) {
		if (Boolean.FALSE.equals(migrationsEnabledRv.getValue())) {
			return;
		}
		try {
			migrationsRv.getValue().apply(true);
		}
		catch (ServiceUnavailableException ex) {
			LOG.error("Cannot apply Neo4j migrations, driver instance cannot reach any database.");
			throw ex;
		}
	}

}
