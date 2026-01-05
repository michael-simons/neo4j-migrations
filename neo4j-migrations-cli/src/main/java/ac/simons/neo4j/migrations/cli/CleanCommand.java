/*
 * Copyright 2020-2026 the original author or authors.
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

import ac.simons.neo4j.migrations.core.CleanResult;
import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * The clean command.
 *
 * @author Michael J. Simons
 * @since 1.1.0
 */
@Command(name = "clean", description = "Removes Neo4j-Migration specific data from the selected schema database.")
final class CleanCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Option(names = "all", defaultValue = "false",
			description = "" + "Set to true to delete all migration chains as well as all Neo4j-Migration "
					+ "constraints and not only the chain for the target database")
	private boolean all;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		CleanResult result = migrations.clean(this.all);
		MigrationsCli.LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(MigrationsCli.LOGGER::warning);
		return 0;
	}

}
