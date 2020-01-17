/*
 * Copyright 2020 the original author or authors.
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

import ac.simons.neo4j.migrations.core.MigrationsConfig;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

/**
 * Base class for a connected command.
 *
 * @author Michael J. Simons
 * @soundtrack Pet Shop Boys - Introspective
 * @since 0.0.5
 */
abstract class ConnectedCommand implements Callable<Integer> {

	/**
	 * Cannot be done on an attribute, as the annotation processor of Picocli prevents those annotations on something
	 * that is not a command.
	 *
	 * @return The migration CLI parent.
	 */
	abstract MigrationsCli getParent();

	@Override
	public Integer call() throws Exception {

		MigrationsCli migrationsCli = getParent();
		try (Driver driver = openConnectionFor(migrationsCli)) {
			MigrationsConfig config = migrationsCli.getConfig();

			return withConfigAndDriver(config, driver);
		} catch (ServiceUnavailableException e) {
			MigrationsCli.LOGGER.log(Level.SEVERE, e.getMessage());
			return CommandLine.ExitCode.SOFTWARE;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * To be implemented by commands that need a connection to a Neo4j server.
	 *
	 * @return The return code of this command
	 * @throws Exception Anything that might happen
	 */
	abstract Integer withConfigAndDriver(MigrationsConfig config, Driver driver) throws Exception;

	private Driver openConnectionFor(MigrationsCli migrationsCli) {

		Config driverConfig = Config.builder().withLogging(Logging.slf4j()).build();
		AuthToken authToken = AuthTokens.basic(migrationsCli.user, new String(migrationsCli.password));
		Driver driver = GraphDatabase.driver(migrationsCli.address, authToken, driverConfig);
		boolean verified = false;
		try {
			driver.verifyConnectivity();
			verified = true;
		} finally {
			// Don't want to rethrow and addiing another frame.
			if (!verified) {
				driver.close();
			}
		}
		return driver;
	}
}
