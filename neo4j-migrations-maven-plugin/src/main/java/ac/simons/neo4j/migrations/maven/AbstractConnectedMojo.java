/*
 * Copyright 2020-2021 the original author or authors.
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
package ac.simons.neo4j.migrations.maven;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsException;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Base class for Neo4j Migrations mojos.
 *
 * @author Michael J. Simons
 * @since 0.0.11
 */
abstract class AbstractConnectedMojo extends AbstractMojo {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	static final Logger LOGGER = Logger.getLogger(AbstractConnectedMojo.class.getName());

	/**
	 * The address this migration should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.
	 */
	@Parameter(required = true, defaultValue = "bolt://localhost:7687")
	private URI address;

	/**
	 * The login of the user connecting to the database.
	 */
	@Parameter(required = true, defaultValue = Defaults.DEFAULT_USER)
	private String user;

	/**
	 * The password of the user connecting to the database.
	 */
	@Parameter(required = true)
	private String password;

	/**
	 * Package to scan. Repeat for multiple packages.
	 */
	@Parameter
	private String[] packagesToScan = new String[0];

	/**
	 * Location to scan. Repeat for multiple locations.
	 */
	@Parameter(defaultValue = "file://${project.build.outputDirectory}/neo4j/migrations")
	private String[] locationsToScan;

	/**
	 * The transaction mode to use.
	 */
	@Parameter(defaultValue = Defaults.TRANSACTION_MODE_VALUE)
	private TransactionMode transactionMode;

	/**
	 * The database that should be migrated (Neo4j EE 4.0+).
	 */
	@Parameter
	private String database;

	/**
	 * The database that should be used for storing informations about migrations (Neo4j EE 4.0+).
	 */
	@Parameter
	private String schemaDatabase;

	/**
	 * An alternative user to impersonate during migration. Might have higher privileges than the user connected, which
	 * will be dropped again after migration. Requires Neo4j EE 4.4+. Leave {@literal null} for using the connected user.
	 */
	@Parameter
	private String impersonatedUser;

	/**
	 * Log the configuration and a couple of other things.
	 */
	@Parameter(defaultValue = "false")
	private boolean verbose;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try (Driver driver = openConnection()) {

			MigrationsConfig config = getConfig();
			Migrations migrations = new Migrations(config, driver);

			withMigrations(migrations);
		} catch (MojoFailureException e) {
			// Don't add stack, but rethrow
			throw e;
		} catch (MigrationsException e) {
			throw new MojoFailureException("Could not execute migrations", e);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	abstract void withMigrations(Migrations migrations) throws MojoFailureException;

	/**
	 * @return The migrations config based on the required options.
	 */
	MigrationsConfig getConfig() {

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan(locationsToScan)
			.withPackagesToScan(packagesToScan)
			.withTransactionMode(transactionMode)
			.withDatabase(database)
			.withSchemaDatabase(schemaDatabase)
			.withImpersonatedUser(impersonatedUser)
			.build();

		config.logTo(LOGGER, verbose);
		return config;
	}

	Driver openConnection() {

		Config driverConfig = Config.builder()
			.withLogging(Logging.console(Level.SEVERE))
			.withUserAgent(Migrations.getUserAgent())
			.build();
		AuthToken authToken = AuthTokens.basic(user, password);
		Driver driver = GraphDatabase.driver(address, authToken, driverConfig);
		boolean verified = false;
		try {
			driver.verifyConnectivity();
			verified = true;
		} finally {
			// Don't want to rethrow and adding another frame.
			if (!verified) {
				driver.close();
			}
		}
		return driver;
	}
}
