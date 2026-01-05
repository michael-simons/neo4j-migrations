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

import java.net.URL;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Applies an explicit lists of migrations.
 *
 * @author Michael J. Simons
 * @since 1.13.0
 */
@Command(name = "run",
		description = "Resolves the specified migrations and applies them. Does not record any metadata.")
final class RunCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Option(names = { "--migration" }, description = "Migration to run. Repeat for multiple migrations.",
			required = true, split = ",", arity = "1..*")
	private URL[] migrationsToRun = new URL[0];

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

		int cnt = migrations.apply(this.migrationsToRun);
		MigrationsCli.LOGGER.log(Level.INFO, "Applied {0} migration(s).", cnt);
		return CommandLine.ExitCode.OK;
	}

}
