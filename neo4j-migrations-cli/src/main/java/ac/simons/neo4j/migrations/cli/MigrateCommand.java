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
package ac.simons.neo4j.migrations.cli;

import static ac.simons.neo4j.migrations.cli.MigrationsCli.*;

import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.Optional;
import java.util.logging.Level;

import org.neo4j.driver.Driver;

/**
 * The migrate command.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
@Command(name = "migrate", description = "Retrieves all pending migrations, verify and applies them.")
final class MigrateCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Override
	public MigrationsCli getParent() {

		return parent;
	}

	@Override
	Integer withConfigAndDriver(MigrationsConfig config, Driver driver) throws Exception {

		Migrations migrations = new Migrations(config, driver);
		Optional<MigrationVersion> lastAppliedMigration = migrations.apply();
		lastAppliedMigration.map(MigrationVersion::getValue)
			.ifPresent(version -> LOGGER.log(Level.INFO, "Database migrated to version {0}.", version));
		return 0;
	}
}
