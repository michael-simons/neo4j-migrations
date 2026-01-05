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

import ac.simons.neo4j.migrations.core.Messages;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * The validate command.
 *
 * @author Michael J. Simons
 * @since 1.2.0
 */
@Command(name = "validate",
		description = "Resolves all local migrations and validates the state of the configured database with them.")
final class ValidateCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		ValidationResult validationResult = migrations.validate();
		MigrationsCli.LOGGER.info(validationResult::prettyPrint);
		boolean isValid = validationResult.isValid();
		if (!isValid) {
			validationResult.getWarnings().forEach(MigrationsCli.LOGGER::info);
			MigrationsCli.LOGGER.info(Messages.INSTANCE.get(validationResult.needsRepair()
					? "validation.database_needs_repair" : "validation.database_is_invalid"));
		}
		return isValid ? 0 : 1;
	}

}
