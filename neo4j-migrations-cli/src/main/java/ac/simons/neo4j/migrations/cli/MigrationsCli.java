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

import static java.util.stream.Collectors.*;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Commandline interface to Neo4j migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
@Command(
	name = "neo4j-migrations",
	mixinStandardHelpOptions = true,
	description = "Migrates Neo4j databases.",
	subcommands = { InfoCommand.class, MigrateCommand.class },
	versionProvider = ManifestVersionProvider.class
)
public final class MigrationsCli implements Runnable {

	static final Logger LOGGER = Logger.getLogger(MigrationsCli.class.getName());

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
		MigrationsCliConsoleHandler handler = new MigrationsCliConsoleHandler();
		List<Logger> loggersToConfigure = Arrays.asList(
			Logger.getAnonymousLogger(), Logger.getGlobal(),
			Logger.getLogger(Migrations.class.getName()), LOGGER);

		for (Logger logger : loggersToConfigure) {
			logger.setUseParentHandlers(false);
			logger.setLevel(Level.INFO);
			logger.addHandler(handler);
		}
	}

	public static void main(String... args) {

		int exitCode = new CommandLine(new MigrationsCli()).execute(args);
		System.exit(exitCode);
	}

	@Option(
		names = { "-a", "--address" },
		description = "The address this migration should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.",
		required = true,
		defaultValue = "bolt://localhost:7687"
	)
	URI address;

	@Option(
		names = { "-u", "--username" },
		description = "The login of the user connecting to the database.",
		required = true,
		defaultValue = "neo4j"
	)
	String user;

	@Option(
		names = { "-p", "--password" },
		description = "The password of the user connecting to the database.",
		required = true,
		arity = "0..1", interactive = true
	)
	char[] password;

	@Option(
		names = { "--package" },
		description = "Package to scan. Repeat for multiple packages."
	)
	private String[] packagesToScan = new String[0];

	@Option(
		names = { "--location" },
		description = "Location to scan. Repeat for multiple locations."
	)
	private String[] locationsToScan = new String[0];

	@Option(
		names = { "--transaction-mode" },
		description = "The transaction mode to use.",
		defaultValue = Defaults.TRANSACTION_MODE_VALUE
	)
	private TransactionMode transactionMode;

	@Option(
		names = { "-d", "--database" },
		description = "The database that should be migrated (Neo4j 4.0+)."
	)
	private String database;

	@Option(
		names = { "-v" },
		description = "Log the configuration and a couple of other things."
	)
	private boolean verbose;

	@Spec
	private CommandSpec commandSpec;

	public void run() {
		throw new CommandLine.ParameterException(commandSpec.commandLine(), "Missing required subcommand");
	}

	/**
	 * @return The migrations config based on the required options.
	 */
	MigrationsConfig getConfig() {

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan(locationsToScan)
			.withPackagesToScan(packagesToScan)
			.withTransactionMode(transactionMode)
			.withDatabase(database)
			.build();

		if (!config.hasPlacesToLookForMigrations()) {
			LOGGER.log(Level.WARNING, "Can't find migrations as neither locations or packages to scan are configured!");
		}

		if (verbose && LOGGER.isLoggable(Level.INFO)) {
			if (config.getDatabase() != null) {
				LOGGER.log(Level.INFO, "Migrations will be applied to using database \"{0}\"", config.getDatabase());
			}
			if (config.getLocationsToScan().length > 0) {
				LOGGER.log(Level.INFO, "Will search for Cypher scripts in \"{0}\"",
					Arrays.stream(config.getLocationsToScan()).collect(joining()));
				LOGGER.log(Level.INFO, "Statements will be applied {0} ",
					config.getTransactionMode() == TransactionMode.PER_MIGRATION ?
						"in one transaction per migration" :
						"in separate transactions");
			}
			if (config.getPackagesToScan().length > 0) {
				LOGGER.log(Level.INFO, "Will scan for Java based migrations in \"{0}\"",
					Arrays.stream(config.getPackagesToScan()).collect(joining()));
			}
		}
		return config;
	}
}
