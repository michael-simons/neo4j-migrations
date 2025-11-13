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
package ac.simons.neo4j.migrations.cli;

import ac.simons.neo4j.migrations.core.DeleteResult;
import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This command can be used to delete individual migrations from the chain of applied
 * migrations. This is useful when migration scripts or classes have been deleted.
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
@Command(name = "delete", description = "Deletes a migration from the chain of applied migrations.")
public class DeleteCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Parameters(paramLabel = "version",
			description = "The full name of the version or the unique name of the version that should be deleted")
	private String versionValue;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	boolean forceSilence() {
		return true;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		MigrationVersion version;
		try {
			version = MigrationVersion.parse(this.versionValue);
		}
		catch (MigrationsException ex) {
			version = MigrationVersion.withValue(this.versionValue);
		}

		DeleteResult result = migrations.delete(version);
		MigrationsCli.LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(MigrationsCli.LOGGER::warning);
		return 0;
	}

}
