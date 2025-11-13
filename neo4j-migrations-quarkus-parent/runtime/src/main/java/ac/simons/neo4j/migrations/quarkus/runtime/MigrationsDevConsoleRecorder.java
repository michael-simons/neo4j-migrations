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

import java.util.function.Supplier;

import ac.simons.neo4j.migrations.core.Migrations;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Triggered during development mode for contributing to the dev console.
 *
 * @author Michael J. Simons
 */
@Recorder
public class MigrationsDevConsoleRecorder {

	/**
	 * Used to expose the migrations runtime value to the dev console.
	 * @param migrationsRv the runtime value containing the migrations
	 * @return a supplier of a runtime object
	 */
	public Supplier<Migrations> recordMigrationsSupplier(RuntimeValue<Migrations> migrationsRv) {

		var migrationDetailsSupplier = new MigrationDetailsSupplier();
		migrationDetailsSupplier.setValue(migrationsRv.getValue());
		return migrationDetailsSupplier;
	}

}
