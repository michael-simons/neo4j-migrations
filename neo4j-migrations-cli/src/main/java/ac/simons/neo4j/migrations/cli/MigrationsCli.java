/*
 * Copyright 2020-2023 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Location;
import ac.simons.neo4j.migrations.core.Location.LocationType;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsException;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.graalvm.nativeimage.ImageInfo;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;

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
	subcommands = {CleanCommand.class, DeleteCommand.class, GenerateCompletion.class, HelpCommand.class, InfoCommand.class, InitCommand.class, MigrateBTreeIndexesCommand.class, MigrateCommand.class, RunCommand.class, ShowCatalogCommand.class, ValidateCommand.class},
	versionProvider = ManifestVersionProvider.class,
	defaultValueProvider = CommonEnvVarDefaultProvider.class
)
public final class MigrationsCli implements Runnable {

	static final Logger LOGGER;
	static final String MIGRATIONS_PROPERTIES_FILENAME = ".migrations.properties";
	static final String OPTION_ADDRESS = "-a";
	static final String OPTION_USERNAME = "-u";
	@SuppressWarnings("squid:S2068") // This is not a password, this is the option to specify one.
	static final String OPTION_PASSWORD = "-p";

	private static final String OPTION_NAME_MAX_CONNECTION_POOL_SIZE = "--max-connection-pool-size";

	static {
		configureLogging();
		LOGGER = Logger.getLogger(MigrationsCli.class.getName());
	}

	@SuppressWarnings("squid:S4792")
	private static void configureLogging() {
		try {
			LogManager.getLogManager()
				.readConfiguration(MigrationsCli.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			throw new MigrationsException("logging.properties are missing. Is your distribution of neo4j-migrations broken?");
		}
	}

	/**
	 * Entry point to the CLI.
	 * @param args The command line arguments
	 */
	public static void main(String... args) {

		CommandLine commandLine = new CommandLine(new MigrationsCli());
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		CommandLine generateCompletionCmd = commandLine.getSubcommands().get("generate-completion");
		generateCompletionCmd.getCommandSpec().usageMessage().hidden(true);

		loadProperties(MIGRATIONS_PROPERTIES_FILENAME)
			.map(CommonEnvVarDefaultProvider::new)
			.ifPresent(commandLine.getCommandSpec()::defaultValueProvider);

		int exitCode = commandLine.execute(args);
		System.exit(exitCode);
	}

	@Option(
		names = { OPTION_ADDRESS, "--address" },
		description = "The address this migration should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.",
		required = true,
		defaultValue = "bolt://localhost:7687"
	)
	private URI address;

	@Option(
		names = { OPTION_USERNAME, "--username" },
		description = "The login of the user connecting to the database.",
		required = true,
		defaultValue = Defaults.DEFAULT_USER
	)
	private String user;

	@Option(names = "--password:file")
	File passwordFile;

	@Option(names = "--password:env")
	String passwordEnv;

	@Option(
		names = { OPTION_PASSWORD, "--password" },
		description = "The password of the user connecting to the database.",
		arity = "0..1", interactive = true
	)
	private char[] password;

	@Option(
		names = { "--package" },
		description = "Package to scan. Repeat for multiple packages.",
		split = ","
	)
	private String[] packagesToScan = new String[0];

	@Option(
		names = { "--location" },
		description = "Location to scan. Repeat for multiple locations.",
		split = ","
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
		description = "The database that should be migrated (Neo4j EE 4.0+)."
	)
	private String database;

	@Option(
		names = { "--schema-database" },
		description = "The database that should be used for storing information about migrations (Neo4j EE 4.0+)."
	)
	private String schemaDatabase;

	@Option(
		names = { "--impersonate" },
		description = "The name of a user to impersonate during migration (Neo4j EE 4.4+)."
	)
	private String impersonatedUser;

	@Option(
		names = { "-v", "--verbose" },
		description = "Log the configuration and a couple of other things."
	)
	private boolean verbose;

	@Option(
		names = { "--validate-on-migrate" },
		description = "Validating helps you verify that the migrations applied to the database match the ones available locally and is on by default.",
		defaultValue = Defaults.VALIDATE_ON_MIGRATE_VALUE
	)
	private boolean validateOnMigrate;

	@Option(
		names = { "--autocrlf" },
		description = "Automatically convert Windows line-endings (CRLF) to LF when reading resource based migrations, pretty much what the same Git option does during checkin.",
		defaultValue = Defaults.AUTOCRLF_VALUE
	)
	private boolean autocrlf;

	@Option(
		names = { OPTION_NAME_MAX_CONNECTION_POOL_SIZE },
		description = "Configure the connection pool size, hardly ever needed to change.",
		defaultValue = "2",
		hidden = true
	)
	private int maxConnectionPoolSize;

	@Spec
	private CommandSpec commandSpec;

	public void run() {
		throw new CommandLine.ParameterException(commandSpec.commandLine(), "Missing required subcommand");
	}

	/**
	 * @return The migrations config based on the required options.
	 * @see #getConfig(boolean)
	 */
	MigrationsConfig getConfig() {
		return getConfig(false);
	}

	/**
	 * @param forceSilence overwrites {@link #verbose} and disables config logging
	 * @return The migrations config based on the required options.
	 */
	MigrationsConfig getConfig(boolean forceSilence) {

		boolean runsInNativeImage = ImageInfo.inImageRuntimeCode();

		if (runsInNativeImage && packagesToScan.length != 0) {
			throw new IllegalArgumentException(
					"Java-based migrations are not supported in native binaries. Please use the Java-based distribution.");
		}

		List<String> classpathLocations = Arrays.stream(locationsToScan)
				.filter(location -> Location.of(location).getType() == LocationType.CLASSPATH)
				.toList();

		if (runsInNativeImage && !classpathLocations.isEmpty()) {
			throw new IllegalArgumentException(
					"Classpath based resource locations are not support in native image: " + String.join(", ", classpathLocations));
		}

		if ((schemaDatabase != null && !schemaDatabase.trim().isEmpty()) && maxConnectionPoolSize < 2) {
			throw new IllegalArgumentException(
					"You must at least allow 2 connections in the pool to use a separate database.");
		}

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan(getOrComputeLocationsToScan())
			.withPackagesToScan(packagesToScan)
			.withTransactionMode(transactionMode)
			.withDatabase(database)
			.withSchemaDatabase(schemaDatabase)
			.withImpersonatedUser(impersonatedUser)
			.withValidateOnMigrate(validateOnMigrate)
			.withAutocrlf(autocrlf)
			.build();

		if (!forceSilence) {
			config.logTo(LOGGER, verbose);
		}
		return config;
	}

	boolean hasLocationsToScan() {
		return locationsToScan.length != 0 || packagesToScan.length != 0;
	}

	String[] getOrComputeLocationsToScan() {
		if (hasLocationsToScan()) {
			return locationsToScan;
		}

		Path defaultPath = Paths.get(Defaults.LOCATIONS_TO_SCAN_WITHOUT_PREFIX).toAbsolutePath();
		if (!Files.isDirectory(defaultPath)) {
			return new String[0];
		}
		String defaultLocationToScan = defaultPath.toUri().toString();
		LOGGER.log(Level.FINE, "Neither locations nor packages to scan configured, using {0}.", defaultLocationToScan);
		return new String[] { defaultLocationToScan };
	}

	AuthToken getAuthToken() {
		Optional<String> resolvedPassword = Optional.empty();
		if (password != null) {
			resolvedPassword = Optional.of(new String(password));
		} else if (passwordEnv != null) {
			resolvedPassword = Optional.ofNullable(System.getenv(passwordEnv));
		} else if (passwordFile != null && passwordFile.isFile()) {
			try {
				resolvedPassword = Optional.of(new String(Files.readAllBytes(passwordFile.toPath())));
			} catch (IOException e) {
				throw new UncheckedIOException("Could not read password file " + passwordFile.getAbsolutePath(), e);
			}
		}

		return resolvedPassword
			.filter(Predicate.not(String::isBlank))
			.map(s -> AuthTokens.basic(user, s))
			.orElseThrow(
				() -> new CommandLine.ParameterException(commandSpec.commandLine(),
					"Missing required option: '--password', '--password:env' or '--password:file'")
			);
	}

	Driver openConnection(AuthToken authToken) {

		Driver driver = GraphDatabase.driver(address, authToken, createDriverConfig());
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

	Config createDriverConfig() {

		return Config.builder()
			.withMaxConnectionPoolSize(maxConnectionPoolSize)
			.withUserAgent(Migrations.getUserAgent())
			.withLogging(Logging.console(Level.SEVERE)).build();
	}

	Properties toProperties() {

		Properties properties = new Properties();
		for (CommandLine.Model.OptionSpec o : commandSpec.options()) {
			if (OPTION_NAME_MAX_CONNECTION_POOL_SIZE.equals(o.longestName())) {
				continue;
			}
			List<String> values = o.stringValues();
			String value = values.isEmpty() ? o.defaultValue() : String.join(",", values);
			if (value != null) {
				String key = o.longestName().replaceAll("^-+", "");
				properties.put(key, value);
			}
		}
		return properties;
	}

	static Optional<Properties> loadProperties(String filename) {

		Path path = Paths.get(filename);
		if (Files.isRegularFile(path)) {
			try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
				Properties properties = new Properties();
				properties.load(in);
				return Optional.of(properties);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "{0} is not readable.", path.toAbsolutePath());
			}
		}
		return Optional.empty();
	}

	void storeProperties(String filename) {

		Path path = Paths.get(filename);
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			toProperties().store(out, null);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "{0} is not writable.", path.toAbsolutePath());
		}
	}
}
