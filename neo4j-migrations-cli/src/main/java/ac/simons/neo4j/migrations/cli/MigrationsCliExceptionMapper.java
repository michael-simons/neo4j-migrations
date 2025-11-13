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

import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.MigrationsException;
import picocli.CommandLine;

/**
 * Define some more or less sane exit codes.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
final class MigrationsCliExceptionMapper implements CommandLine.IExitCodeExceptionMapper {

	@Override
	public int getExitCode(Throwable exception) {

		if (exception instanceof org.neo4j.driver.exceptions.ServiceUnavailableException) {
			return CommandLine.ExitCode.SOFTWARE;
		}
		else if (exception instanceof MigrationsException) {
			return CommandLine.ExitCode.USAGE;
		}
		else {
			MigrationsCli.LOGGER.log(Level.WARNING, "Uncaught exception.", exception);
			return CommandLine.ExitCode.USAGE;
		}
	}

}
