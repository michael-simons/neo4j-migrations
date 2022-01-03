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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import org.neo4j.driver.Driver;

/**
 * Records both initialization of the {@link MigrationsConfig migration config} and the {@link Migrations migrations} itself.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@Recorder
public class MigrationsRecorder {

	public RuntimeValue<MigrationsConfig> initializeConfig(MigrationsProperties migrationsProperties) {

		var config = MigrationsConfig.builder()
			.withLocationsToScan(migrationsProperties.locationsToScan.toArray(String[]::new))
			.withPackagesToScan(migrationsProperties.packagesToScan.map(v -> v.toArray(String[]::new)).orElse(null))
			.withTransactionMode(migrationsProperties.transactionMode)
			.withDatabase(migrationsProperties.database.orElse(null))
			.withSchemaDatabase(migrationsProperties.schemaDatabase.orElse(null))
			.withImpersonatedUser(migrationsProperties.impersonatedUser.orElse(null))
			.withInstalledBy(migrationsProperties.installedBy.orElse(null))
			.withValidateOnMigrate(migrationsProperties.validateOnMigrate)
			.withAutocrlf(migrationsProperties.autocrlf)
			.build();

		return new RuntimeValue<>(config);
	}

	public RuntimeValue<MigrationsEnabled> isEnabled(MigrationsProperties migrationsProperties) {
		return new RuntimeValue<>(new MigrationsEnabled(migrationsProperties.enabled));
	}

	public RuntimeValue<Migrations> initializeMigrations(
		RuntimeValue<MigrationsConfig> migrationsConfig, RuntimeValue<Driver> driver
	) {
		return new RuntimeValue<>(new Migrations(migrationsConfig.getValue(), driver.getValue()));
	}
}
