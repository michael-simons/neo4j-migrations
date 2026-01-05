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

import java.util.concurrent.Callable;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.FatalDiscoveryException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import picocli.CommandLine;

/**
 * Base class for a connected command.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
abstract class ConnectedCommand implements Callable<Integer> {

	static boolean isCatalogException(Throwable e) {
		if (!(e instanceof IllegalArgumentException || e instanceof IllegalStateException)) {
			return false;
		}

		var stackTrace = e.getStackTrace();
		if (stackTrace.length == 0) {
			return false;
		}

		return stackTrace[0].getClassName().contains(Renderer.class.getPackageName());
	}

	/**
	 * Cannot be done on an attribute, as the annotation processor of Picocli prevents
	 * those annotations on something that is not a command.
	 * @return the migration CLI parent.
	 */
	abstract MigrationsCli getParent();

	/**
	 * {@return true to enforce silence (no complaining about missing locations)}
	 */
	boolean forceSilence() {
		return false;
	}

	@Override
	public Integer call() {

		MigrationsCli migrationsCli = getParent();
		AuthToken authToken = migrationsCli.getAuthToken();

		MigrationsConfig config;
		try {
			config = migrationsCli.getConfig(forceSilence());
		}
		catch (IllegalArgumentException ex) {
			MigrationsCli.LOGGER.log(Level.SEVERE, ex.getMessage());
			return CommandLine.ExitCode.USAGE;
		}

		try (Driver driver = migrationsCli.openConnection(authToken)) {

			Migrations migrations = new Migrations(config, driver);

			return withMigrations(migrations);
		}
		catch (AuthenticationException | ServiceUnavailableException | FatalDiscoveryException ex) {
			MigrationsCli.LOGGER.log(Level.SEVERE, ex.getMessage());
			return CommandLine.ExitCode.SOFTWARE;
		}
		catch (MigrationsException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ClientException ce) {
				MigrationsCli.LOGGER.log(Level.SEVERE, "{0}{1}\t{2}: {3}",
						new Object[] { ex.getMessage(), System.lineSeparator(), ce.code(), cause.getMessage() });
			}
			else if (isCatalogException(cause)) {
				MigrationsCli.LOGGER.log(Level.SEVERE, "{0}: {1}",
						new Object[] { ex.getMessage().replaceAll("\\.$", ""), cause.getMessage() });
			}
			else {
				MigrationsCli.LOGGER.log(Level.SEVERE, ex.getMessage());
			}
			return CommandLine.ExitCode.SOFTWARE;
		}
	}

	/**
	 * To be implemented by commands that need a connection to a Neo4j server.
	 * @param migrations the migrations to work on
	 * @return the return code of this command
	 */
	abstract Integer withMigrations(Migrations migrations);

}
