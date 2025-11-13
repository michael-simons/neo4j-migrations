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
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ac.simons.neo4j.migrations.cli.internal.ImageInfo;
import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Location;
import ac.simons.neo4j.migrations.core.Location.LocationType;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.CypherVersion;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsConfig.VersionSortOrder;
import ac.simons.neo4j.migrations.core.MigrationsException;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Commandline interface to Neo4j migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
@Command(name = "neo4j-migrations", mixinStandardHelpOptions = true, description = "Migrates Neo4j databases.",
		subcommands = { CleanCommand.class, DeleteCommand.class, GenerateCompletion.class, HelpCommand.class,
				InfoCommand.class, InitCommand.class, MigrateBTreeIndexesCommand.class, MigrateCommand.class,
				RunCommand.class, ShowCatalogCommand.class, ValidateCommand.class, RepairCommand.class },
		versionProvider = ManifestVersionProvider.class, defaultValueProvider = CommonEnvVarDefaultProvider.class)
public final class MigrationsCli implements Runnable {

	static final Logger LOGGER;
	static final String MIGRATIONS_PROPERTIES_FILENAME = ".migrations.properties";
	static final String OPTION_ADDRESS = "-a";
	static final String OPTION_USERNAME = "-u";

	// This is not a password, this is the option to specify one.
	@SuppressWarnings("squid:S2068")
	static final String OPTION_PASSWORD = "-p";

	private static final String OPTION_NAME_MAX_CONNECTION_POOL_SIZE = "--max-connection-pool-size";

	static {
		configureLogging();
		LOGGER = Logger.getLogger(MigrationsCli.class.getName());
	}

	@Option(names = "--password:file")
	File passwordFile;

	@Option(names = "--password:env")
	String passwordEnv;

	@Option(names = "--custom-auth")
	Map<String, String> customAuth;

	@Option(names = "--bearer",
			description = "A bearer token to be used as an alternative to a username/password token.")
	String bearer;

	@Option(names = { OPTION_ADDRESS, "--address" },
			description = "The address this migration should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.",
			required = true, defaultValue = "bolt://localhost:7687")
	private URI address;

	@Option(names = { OPTION_USERNAME, "--username" },
			description = "The login of the user connecting to the database.", required = true,
			defaultValue = Defaults.DEFAULT_USER)
	private String user;

	@Option(names = { OPTION_PASSWORD, "--password" },
			description = "The password of the user connecting to the database.", arity = "0..1", interactive = true)
	private char[] password;

	@Option(names = { "--package" }, description = "Package to scan. Repeat for multiple packages.", split = ",")
	private String[] packagesToScan = new String[0];

	@Option(names = { "--location" }, description = "Location to scan. Repeat for multiple locations.", split = ",")
	private String[] locationsToScan = new String[0];

	@Option(names = { "--transaction-mode" }, description = "The transaction mode to use.",
			defaultValue = Defaults.TRANSACTION_MODE_VALUE)
	private TransactionMode transactionMode;

	@Option(names = { "--transaction-timeout" },
			description = "Configures the transaction timeout that should be applied for each migration or each statement.")
	private Duration transactionTimeout;

	@Option(names = { "-d", "--database" }, description = "The database that should be migrated (Neo4j EE 4.0+).")
	private String database;

	@Option(names = { "--schema-database" },
			description = "The database that should be used for storing information about migrations (Neo4j EE 4.0+).")
	private String schemaDatabase;

	@Option(names = { "--impersonate" },
			description = "The name of a user to impersonate during migration (Neo4j EE 4.4+).")
	private String impersonatedUser;

	@Option(names = { "-v", "--verbose" }, description = "Log the configuration and a couple of other things.")
	private boolean verbose;

	@Option(names = { "--validate-on-migrate" },
			description = "Validating helps you verify that the migrations applied to the database match the ones available locally and is on by default.",
			defaultValue = Defaults.VALIDATE_ON_MIGRATE_VALUE)
	private boolean validateOnMigrate;

	@Option(names = { "--autocrlf" },
			description = "Automatically convert Windows line-endings (CRLF) to LF when reading resource based migrations, pretty much what the same Git option does during checkin.",
			defaultValue = Defaults.AUTOCRLF_VALUE)
	private boolean autocrlf;

	@Option(names = { OPTION_NAME_MAX_CONNECTION_POOL_SIZE },
			description = "Configure the connection pool size, hardly ever needed to change.", defaultValue = "2",
			hidden = true)
	private int maxConnectionPoolSize;

	@Option(names = { "--delay-between-migrations" },
			description = "A configurable delay that will be applied in between applying two migrations.")
	private Duration delayBetweenMigrations;

	@Option(names = { "--version-sort-order" },
			description = "How versions are supposed to be sorted (lexicographic or semantic)",
			defaultValue = Defaults.VERSION_SORT_ORDER_VALUE)
	private VersionSortOrder versionSortOrder;

	@Option(names = { "--cypher-version" },
			description = "A valid Cypher version to prepend to every statement of every Cypher based migration",
			defaultValue = Defaults.CYPHER_VERSION_VALUE)
	private CypherVersion cypherVersion;

	@Option(names = { "--out-of-order" },
			description = "Use this flag to enable migrations to be discovered out-of-order and integrated into the migration chain.",
			defaultValue = Defaults.OUT_OF_ORDER_VALUE)
	private boolean outOfOrder;

	@Option(names = { "--target" },
			description = "Use this option to specify a valid target version up to which migrations should be considered. Can also be one of current, latest or next.")
	private String target;

	@Option(names = { "--use-flyway-compatible-checksums" },
			description = "Use this flag to enable Flyway compatible checksums.",
			defaultValue = Defaults.USE_FLYWAY_COMPATIBLE_CHECKSUMS_VALUE)
	private boolean useFlywayCompatibleChecksums;

	@Spec
	private CommandSpec commandSpec;

	@SuppressWarnings("squid:S4792")
	private static void configureLogging() {
		try {
			LogManager.getLogManager()
				.readConfiguration(MigrationsCli.class.getResourceAsStream("/logging.properties"));
		}
		catch (IOException ex) {
			throw new MigrationsException(
					"logging.properties are missing. Is your distribution of neo4j-migrations broken?");
		}
	}

	/**
	 * Entry point to the CLI.
	 * @param args the command line arguments
	 */
	public static void main(String... args) {

		CommandLine commandLine = new CommandLine(new MigrationsCli());
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		CommandLine generateCompletionCmd = commandLine.getSubcommands().get("generate-completion");
		generateCompletionCmd.getCommandSpec().usageMessage().hidden(true);

		commandLine.setHelpFactory(OrderedCommandLineHelp::new);

		loadProperties(MIGRATIONS_PROPERTIES_FILENAME).map(CommonEnvVarDefaultProvider::new)
			.ifPresent(commandLine.getCommandSpec()::defaultValueProvider);

		int exitCode = commandLine.execute(args);
		System.exit(exitCode);
	}

	static Optional<Properties> loadProperties(String filename) {

		Path path = Paths.get(filename);
		if (Files.isRegularFile(path)) {
			try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
				Properties properties = new Properties();
				properties.load(in);
				return Optional.of(properties);
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, "{0} is not readable.", path.toAbsolutePath());
			}
		}
		return Optional.empty();
	}

	public void run() {
		throw new CommandLine.ParameterException(this.commandSpec.commandLine(), "Missing required subcommand");
	}

	/**
	 * {@return the migrations config based on the required options}
	 * @see #getConfig(boolean)
	 */
	MigrationsConfig getConfig() {
		return getConfig(false);
	}

	/**
	 * Retrieves a new configuration.
	 * @param forceSilence overwrites {@link #verbose} and disables config logging
	 * @return the migrations config based on the required options.
	 */
	MigrationsConfig getConfig(boolean forceSilence) {

		boolean runsInNativeImage = ImageInfo.inImageRuntimeCode();

		if (runsInNativeImage && this.packagesToScan.length != 0) {
			throw new IllegalArgumentException(
					"Java-based migrations are not supported in native binaries. Please use the Java-based distribution.");
		}

		List<String> classpathLocations = Arrays.stream(this.locationsToScan)
			.filter(location -> Location.of(location).getType() == LocationType.CLASSPATH)
			.toList();

		if (runsInNativeImage && !classpathLocations.isEmpty()) {
			throw new IllegalArgumentException("Classpath based resource locations are not support in native image: "
					+ String.join(", ", classpathLocations));
		}

		if ((this.schemaDatabase != null && !this.schemaDatabase.trim().isEmpty()) && this.maxConnectionPoolSize < 2) {
			throw new IllegalArgumentException(
					"You must at least allow 2 connections in the pool to use a separate database.");
		}

		MigrationsConfig config = MigrationsConfig.builder()
			.withLocationsToScan(getOrComputeLocationsToScan())
			.withPackagesToScan(this.packagesToScan)
			.withTransactionMode(this.transactionMode)
			.withTransactionTimeout(this.transactionTimeout)
			.withDatabase(this.database)
			.withSchemaDatabase(this.schemaDatabase)
			.withImpersonatedUser(this.impersonatedUser)
			.withValidateOnMigrate(this.validateOnMigrate)
			.withAutocrlf(this.autocrlf)
			.withDelayBetweenMigrations(this.delayBetweenMigrations)
			.withVersionSortOrder(this.versionSortOrder)
			.withOutOfOrderAllowed(this.outOfOrder)
			.withFlywayCompatibleChecksums(this.useFlywayCompatibleChecksums)
			.withTarget(this.target)
			.withCypherVersion(this.cypherVersion)
			.build();

		if (!forceSilence) {
			config.logTo(LOGGER, this.verbose);
		}
		return config;
	}

	boolean hasLocationsToScan() {
		return this.locationsToScan.length != 0 || this.packagesToScan.length != 0;
	}

	String[] getOrComputeLocationsToScan() {
		if (hasLocationsToScan()) {
			return this.locationsToScan;
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
		if (this.password != null) {
			resolvedPassword = Optional.of(new String(this.password));
		}
		else if (this.passwordEnv != null) {
			resolvedPassword = Optional.ofNullable(System.getenv(this.passwordEnv));
		}
		else if (this.passwordFile != null && this.passwordFile.isFile()) {
			try {
				resolvedPassword = Optional.of(new String(Files.readAllBytes(this.passwordFile.toPath())));
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Could not read password file " + this.passwordFile.getAbsolutePath(),
						ex);
			}
		}

		return resolvedPassword.filter(Predicate.not(String::isBlank))
			.map(s -> AuthTokens.basic(this.user, s))
			.or(this::getOptionalBearerToken)
			.or(this::getOptionalCustomToken)
			.orElseThrow(() -> new CommandLine.ParameterException(this.commandSpec.commandLine(),
					"Missing required option: '--password', '--password:env', '--password:file', '--bearer' or `--custom-auth`"));
	}

	Optional<AuthToken> getOptionalCustomToken() {
		if (this.customAuth == null || this.customAuth.isEmpty()) {
			return Optional.empty();
		}

		var copy = new HashMap<String, Object>(this.customAuth);
		var principal = Objects.requireNonNull(Optional.ofNullable(findAndRemove(copy, "principal")).orElse(this.user),
				"Principal for custom auth must not be null");
		var credentials = Objects.requireNonNull(findAndRemove(copy, "credentials"),
				"Credentials for custom auth must not be null");
		var realm = findAndRemove(copy, "realm");
		var scheme = Objects.requireNonNull(findAndRemove(copy, "scheme"), "Scheme for custom auth must not be null");

		return Optional.of(AuthTokens.custom(principal, credentials, realm, scheme, copy.isEmpty() ? null : copy));
	}

	String findAndRemove(Map<String, Object> src, String key) {
		var it = src.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			if (entry.getKey().toLowerCase(Locale.ROOT).equals(key)) {
				it.remove();
				return (String) entry.getValue();
			}
		}
		return null;
	}

	Optional<AuthToken> getOptionalBearerToken() {
		if (this.bearer == null || this.bearer.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(AuthTokens.bearer(this.bearer.trim()));
	}

	Driver openConnection(AuthToken authToken) {

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

	Config createDriverConfig() {

		return Config.builder()
			.withMaxConnectionPoolSize(this.maxConnectionPoolSize)
			.withUserAgent(Migrations.getUserAgent())
			.build();
	}

	Properties toProperties() {

		Properties properties = new Properties();
		for (CommandLine.Model.OptionSpec o : this.commandSpec.options()) {
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

	void storeProperties(String filename) {

		Path path = Paths.get(filename);
		try (OutputStream out = new BufferedOutputStream(
				Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			toProperties().store(out, null);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "{0} is not writable.", path.toAbsolutePath());
		}
	}

}
