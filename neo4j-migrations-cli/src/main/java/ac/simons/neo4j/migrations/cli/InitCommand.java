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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import ac.simons.neo4j.migrations.core.Defaults;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Initializes a new directory containing migrations.
 *
 * @author Michael J. Simons
 * @since 1.4.0
 */
@Command(name = "init", description = "Creates a migration project inside the current folder.")
final class InitCommand implements Callable<Integer> {

	@ParentCommand
	private MigrationsCli parent;

	@Override
	public Integer call() throws Exception {

		if (!this.parent.hasLocationsToScan()) {
			Files.createDirectories(Paths.get(Defaults.LOCATIONS_TO_SCAN_WITHOUT_PREFIX));
		}

		this.parent.storeProperties(MigrationsCli.MIGRATIONS_PROPERTIES_FILENAME);
		return CommandLine.ExitCode.OK;
	}

}
