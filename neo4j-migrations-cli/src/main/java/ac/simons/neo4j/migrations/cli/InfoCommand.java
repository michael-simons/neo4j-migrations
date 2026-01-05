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

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * The info command.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
@Command(name = "info", description = "Retrieves all applied and pending information, prints them and exits.")
final class InfoCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Option(names = "mode", defaultValue = "COMPARE",
			description = "Controls how the information should be computed. Valid options are ${COMPLETION-CANDIDATES} with ${DEFAULT-VALUE} being the default. "
					+ "${DEFAULT-VALUE} will always compare locally discovered and remotely applied migrations, while the other options just check what's there.")
	private MigrationChain.ChainBuilderMode mode = MigrationChain.ChainBuilderMode.COMPARE;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		MigrationChain migrationChain = migrations.info(this.mode);
		MigrationsCli.LOGGER.info(migrationChain::prettyPrint);
		return 0;
	}

}
