/*
 * Copyright 2020-2023 the original author or authors.
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
 * @since TBA
 */
@Command(name = "repair", description = "Repairs the database....text needed")
final class RepairCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Override
	public MigrationsCli getParent() {
		return parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		RepairmentResult result = migrations.repair();
		MigrationsCli.LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(MigrationsCli.LOGGER::warning);
		return 0;
	}
}
