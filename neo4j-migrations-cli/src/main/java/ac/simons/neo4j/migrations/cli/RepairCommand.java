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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.RepairmentResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * The repair command.
 *
 * @author Gerrit Meier
 * @since 2.2.0
 */
@Command(name = "repair", description = ""
		+ "Compares locally discovered migrations with the remote chain and repairs the remote chain if necessary; "
		+ "no migrations will be applied during this process, only the migration chain will be manipulated. "
		+ "This command requires at least one local migration.")
final class RepairCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		RepairmentResult result = migrations.repair();
		MigrationsCli.LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(MigrationsCli.LOGGER::warning);
		return 0;
	}

}
