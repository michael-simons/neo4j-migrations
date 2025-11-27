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
package ac.simons.neo4j.migrations.maven;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.CypherVersion;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsConfig.VersionSortOrder;
import ac.simons.neo4j.migrations.core.MigrationsException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Base class for Neo4j Migrations mojos.
 *
 * @author Michael J. Simons
 * @since 0.0.11
 */
abstract class AbstractConnectedMojo extends AbstractMojo {

	static final Logger LOGGER = Logger.getLogger(AbstractConnectedMojo.class.getName());

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	/**
	 * The address this migration should connect to. The driver supports bolt,
	 * bolt+routing or neo4j as schemes.
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
	 * Configures the transaction timeout that should be applied for each migration or
	 * each statement (the latter depends on {@link #transactionMode}). {@literal null} is
	 * a valid value and make the driver apply the default timeout for the database.
	 * <p>
	 * The value must be a valid ISO-8601 duration representation.
	 *
	 * @since 2.13.0
	 */
	@Parameter
	private String transactionTimeout;

	/**
	 * The database that should be migrated (Neo4j EE 4.0+).
	 */
	@Parameter
	private String database;

	/**
	 * The database that should be used for storing informations about migrations (Neo4j
	 * EE 4.0+).
	 */
	@Parameter
	private String schemaDatabase;

	/**
	 * An alternative user to impersonate during migration. Might have higher privileges
	 * than the user connected, which will be dropped again after migration. Requires
	 * Neo4j EE 4.4+. Leave {@literal null} for using the connected user.
	 */
	@Parameter
	private String impersonatedUser;

	/**
	 * Log the configuration and a couple of other things.
	 */
	@Parameter(defaultValue = "false")
	private boolean verbose;

	/**
	 * The sort order to use.
	 */
	@Parameter(defaultValue = Defaults.VERSION_SORT_ORDER_VALUE)
	private VersionSortOrder versionSortOrder;

	/**
	 * Whether to allow out-of-order migrations or not.
	 * @since 2.14.0
	 */
	@Parameter(defaultValue = Defaults.OUT_OF_ORDER_VALUE)
	private boolean outOfOrder;

	/**
	 * Whether to use Flyway compatible checksums or not.
	 * @since 2.17.0
	 */
	@Parameter(defaultValue = Defaults.USE_FLYWAY_COMPATIBLE_CHECKSUMS_VALUE)
	private boolean useFlywayCompatibleChecksums;

	/**
	 * Use this property to configure a Cypher version that will be prepended to every
	 * statement in every migration found. Leave it {@literal null} or use
	 * {@link CypherVersion#DATABASE_DEFAULT} (the default), the keep the existing
	 * behaviour of letting the database decide.
	 *
	 * @since 2.19.0
	 */
	@Parameter(defaultValue = Defaults.CYPHER_VERSION_VALUE)
	private CypherVersion cypherVersion;

	/**
	 * Use this option to specify a valid target version up to which migrations should be
	 * considered. Can also be one of current, latest or next.
	 *
	 * @since 2.15.0
	 */
	@Parameter
	private String target;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try (Driver driver = openConnection()) {

			MigrationsConfig config = getConfig();
			Migrations migrations = new Migrations(config, driver);

			withMigrations(migrations);
		}
		catch (MojoFailureException ex) {
			// Don't add stack, but rethrow
			throw ex;
		}
		catch (MigrationsException ex) {
			throw new MojoFailureException("Could not execute migrations", ex);
		}
		catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	abstract void withMigrations(Migrations migrations) throws MojoFailureException;

	/**
	 * {@return the migrations config based on the required options}
	 */
	MigrationsConfig getConfig() {

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan(this.locationsToScan)
			.withPackagesToScan(this.packagesToScan)
			.withTransactionMode(this.transactionMode)
			.withTransactionTimeout(Optional.ofNullable(this.transactionTimeout).map(Duration::parse).orElse(null))
			.withDatabase(this.database)
			.withSchemaDatabase(this.schemaDatabase)
			.withImpersonatedUser(this.impersonatedUser)
			.withVersionSortOrder(this.versionSortOrder)
			.withOutOfOrderAllowed(this.outOfOrder)
			.withFlywayCompatibleChecksums(this.useFlywayCompatibleChecksums)
			.withTarget(this.target)
			.withCypherVersion(this.cypherVersion)
			.build();

		config.logTo(LOGGER, this.verbose);
		return config;
	}

	Driver openConnection() {

		AuthToken authToken = AuthTokens.basic(this.user, this.password);
		Driver driver = GraphDatabase.driver(this.address, authToken, createDriverConfig());
		boolean verified = false;
		try {
			driver.verifyConnectivity();
			verified = true;
		}
		finally {
			// Don't want to rethrow and adding another frame.
			if (!verified) {
				driver.close();
			}
		}
		return driver;
	}

	static Config createDriverConfig() {

		var logger = Logger.getLogger("org.neo4j.driver");
		logger.addHandler(new ConsoleHandler());
		var lvl = Level.SEVERE;
		logger.setLevel(lvl);
		for (var handler : logger.getHandlers()) {
			handler.setLevel(lvl);
		}
		return Config.builder().withUserAgent(Migrations.getUserAgent()).build();
	}

}
