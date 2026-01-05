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
package ac.simons.neo4j.migrations.core;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.internal.Strings;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.DatabaseInfo;

/**
 * Configuration for Migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class MigrationsConfig {

	private final String[] packagesToScan;

	private final String[] locationsToScan;

	private final TransactionMode transactionMode;

	/**
	 * The database to migrate.
	 */
	private final String database;

	/**
	 * The database to store the schema in.
	 */
	private final String schemaDatabase;

	private final String impersonatedUser;

	private final String installedBy;

	private final boolean validateOnMigrate;

	private final boolean autocrlf;

	private final Discoverer<JavaBasedMigration> migrationClassesDiscoverer;

	private final ClasspathResourceScanner resourceScanner;

	/**
	 * A configurable delay that will be applied in between applying two migrations.
	 */
	private final Duration delayBetweenMigrations;

	private final List<? extends RenderConfig.AdditionalRenderingOptions> constraintOptions;

	private final VersionSortOrder versionSortOrder;

	private final Duration transactionTimeout;

	private final boolean outOfOrder;

	private final String target;

	private final boolean useFlywayCompatibleChecksums;

	private final CypherVersion cypherVersion;

	private MigrationsConfig(Builder builder) {

		this.packagesToScan = (builder.packagesToScan != null) ? builder.packagesToScan
				: Defaults.PACKAGES_TO_SCAN.toArray(new String[0]);
		this.locationsToScan = (builder.locationsToScan != null) ? builder.locationsToScan
				: Defaults.LOCATIONS_TO_SCAN.toArray(new String[0]);
		this.transactionMode = Optional.ofNullable(builder.transactionMode).orElse(TransactionMode.PER_MIGRATION);
		this.database = builder.database;
		this.impersonatedUser = builder.impersonatedUser;
		this.installedBy = Optional.ofNullable(builder.installedBy).orElse(System.getProperty("user.name"));
		this.validateOnMigrate = builder.validateOnMigrate;
		this.autocrlf = builder.autocrlf;
		this.migrationClassesDiscoverer = (builder.migrationClassesDiscoverer != null)
				? builder.migrationClassesDiscoverer : new JavaBasedMigrationDiscoverer();
		this.resourceScanner = (builder.resourceScanner != null) ? builder.resourceScanner
				: new DefaultClasspathResourceScanner();
		this.schemaDatabase = builder.schemaDatabase;
		this.delayBetweenMigrations = builder.delayBetweenMigrations;
		this.constraintOptions = builder.constraintOptions;
		this.versionSortOrder = builder.versionSortOrder;
		this.transactionTimeout = builder.transactionTimeout;
		this.outOfOrder = builder.outOfOrder;
		this.useFlywayCompatibleChecksums = builder.useFlywayCompatibleChecksums;
		if (builder.target == null || builder.target.isBlank()) {
			this.target = null;
		}
		else {
			this.target = builder.target;
		}
		this.cypherVersion = (builder.cypherVersion != null) ? builder.cypherVersion : Defaults.CYPHER_VERSION;
	}

	/**
	 * Start building a new configuration.
	 * @return the entry point for creating a new configuration.
	 * @since 0.0.1
	 */
	public static Builder builder() {

		return new Builder();
	}

	/**
	 * {@return the default config}
	 * @since 0.0.6
	 */
	public static MigrationsConfig defaultConfig() {

		return builder().build();
	}

	/**
	 * {@return the list of packages to scan}
	 */
	public String[] getPackagesToScan() {
		return this.packagesToScan;
	}

	/**
	 * {@return the list of locations to scan}
	 */
	public String[] getLocationsToScan() {
		return this.locationsToScan;
	}

	/**
	 * Returns the transaction mode (whether to use one transaction for per migration or
	 * per statement)}.
	 * @return the transaction mode
	 */
	public TransactionMode getTransactionMode() {
		return this.transactionMode;
	}

	/**
	 * Returns an optional target database.
	 * @return an optional target database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalDatabase() {
		return Strings.optionalOf(this.database);
	}

	/**
	 * Returns an optional schema database.
	 * @return an optional schema database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalSchemaDatabase() {
		return Strings.optionalOf(this.schemaDatabase);
	}

	/**
	 * Returns an optional user to impersonate.
	 * @return an optional user to impersonate
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalImpersonatedUser() {
		return Strings.optionalOf(this.impersonatedUser);
	}

	/**
	 * Returns optional user information about the user executing the migration.
	 * @return optional user information about the user executing the migration
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalInstalledBy() {
		return Strings.optionalOf(this.installedBy);
	}

	/**
	 * {@return true if a validation should run before migrations are applied}
	 */
	public boolean isValidateOnMigrate() {
		return this.validateOnMigrate;
	}

	/**
	 * Configures whether <code>CRLF</code> line endings should be automatically converted
	 * into <code>LF</code>.
	 * @return true to automatically convert line endings
	 */
	public boolean isAutocrlf() {
		return this.autocrlf;
	}

	/**
	 * Returns the discoverer for class based migrations, never {@literal null}.
	 * @return the discoverer for class based migrations, never {@literal null}
	 * @since 1.3.0
	 */
	public Discoverer<JavaBasedMigration> getMigrationClassesDiscoverer() {
		return this.migrationClassesDiscoverer;
	}

	/**
	 * Returns the resource scanner, never {@literal null}.
	 * @return the resource scanner, never {@literal null}
	 * @since 1.3.0
	 */
	public ClasspathResourceScanner getResourceScanner() {
		return this.resourceScanner;
	}

	/**
	 * Returns the delay to apply between migrations.
	 * @return the delay to apply between migrations
	 * @since 2.3.2
	 */
	public Optional<Duration> getOptionalDelayBetweenMigrations() {
		return Optional.ofNullable(this.delayBetweenMigrations);
	}

	/**
	 * {@return the list of additional options to use when rendering constraints}
	 * @since 2.8.2
	 */
	// wild card on purpose, dear sonar
	@SuppressWarnings({ "squid:S1452" })
	public List<? extends RenderConfig.AdditionalRenderingOptions> getConstraintRenderingOptions() {
		return List.copyOf(this.constraintOptions);
	}

	/**
	 * Returns the transaction timeout, <code>null</code> indicates all transactions will
	 * use the drivers default timeout.
	 * @return the transaction tineout
	 * @since 2.13.0
	 */
	public Duration getTransactionTimeout() {
		return this.transactionTimeout;
	}

	/**
	 * When this flag is set to {@literal true}, new migrations discovered that are "out
	 * of order", such as a version 15 is to be found between 10 and 20, it will be
	 * accepted, integrated into the chain and then move on instead of throwing an error.
	 * @return true if migrations shall be allowed to be out of order
	 * @since 2.14.0
	 */
	public boolean isOutOfOrder() {
		return this.outOfOrder;
	}

	/**
	 * {@return a valid target version or one of three dedicated values}
	 * @since 2.15.0
	 */
	public String getTarget() {
		return this.target;
	}

	/**
	 * {@return if Flyway compatible checksums should be used}
	 * @since 2.17.0
	 */
	public boolean isUseFlywayCompatibleChecksums() {
		return this.useFlywayCompatibleChecksums;
	}

	/**
	 * {@return the cypher version used as prefix for all Cypher scripts}
	 * @since 2.19.0
	 */
	public CypherVersion getCypherVersion() {
		return this.cypherVersion;
	}

	/**
	 * Helper method to pretty print this configuration into a logger (on level
	 * {@literal INFO} respectively {@literal WARNING}.
	 * @param logger the logger to print to
	 * @param verbose set to {@literal true} if you want to print all details
	 */
	public void logTo(Logger logger, boolean verbose) {
		if (!this.hasPlacesToLookForMigrations()) {
			logger.log(Level.WARNING,
					"Cannot find migrations as neither locations nor packages to scan are configured!");
		}

		if (verbose && logger.isLoggable(Level.INFO)) {
			this.getOptionalDatabase()
				.ifPresent(v -> logger.log(Level.INFO, "Migrations will be applied to database \"{0}\"", v));
			if (this.getLocationsToScan().length > 0) {
				logger.log(Level.INFO, "Will search for Cypher scripts in \"{0}\"",
						String.join("", this.getLocationsToScan()));
				logger.log(Level.INFO, "Statements will be applied {0}",
						(this.getTransactionMode() == TransactionMode.PER_MIGRATION)
								? "in one transaction per migration" : "in separate transactions");
			}
			if (this.getPackagesToScan().length > 0) {
				logger.log(Level.INFO, "Will scan for Java-based migrations in \"{0}\"",
						String.join("", this.getPackagesToScan()));
			}
		}
	}

	/**
	 * This is internal API and will be made package private in 2.0.0.
	 * @return true if there are packages to scan
	 */
	boolean hasPlacesToLookForMigrations() {
		return this.getPackagesToScan().length > 0 || this.getLocationsToScan().length > 0;
	}

	/**
	 * The migration target will be empty if no target database is given or the schema
	 * database is the same as the target.
	 * @param context the context in which the target should be retrieved
	 * @return a target database to use for all chains stored.
	 */
	Optional<String> getMigrationTargetIn(MigrationContext context) {
		Optional<String> optionalDatabase = getOptionalDatabase();
		Optional<String> optionalSchemaDatabase = getOptionalSchemaDatabase();

		if (optionalSchemaDatabase.isEmpty()) {
			return Optional.empty();
		}

		if (optionalDatabase.isEmpty()) {
			// We need to connect to get this information
			try (Session session = context.getSession()) {
				try {
					optionalDatabase = Optional.of(session
						.executeRead(tx -> tx.run("CALL db.info() YIELD name").single().get("name").asString()));
				}
				catch (ClientException ex) {
					optionalDatabase = Optional
						.ofNullable(session.executeRead(tx -> tx.run("MATCH (n) RETURN count(n)").consume()).database())
						.map(DatabaseInfo::name);
				}
			}
		}

		if (!optionalDatabase.equals(optionalSchemaDatabase)) {
			return optionalDatabase.map(s -> s.toLowerCase(Locale.ROOT));
		}
		return Optional.empty();
	}

	/**
	 * {@return the configured version sort order}
	 */
	public VersionSortOrder getVersionSortOrder() {
		return this.versionSortOrder;
	}

	Comparator<MigrationVersion> getVersionComparator() {
		return MigrationVersion.getComparator(this.versionSortOrder);
	}

	/**
	 * Used for configuring the transaction mode in Cypher-based transactions.
	 */
	public enum TransactionMode {

		/**
		 * Run all statements in one transaction. May need more memory, but it's generally
		 * safer. Either the migration runs as a whole or not at all.
		 */
		PER_MIGRATION,
		/**
		 * Runs each statement in a separate transaction. May leave your database in an
		 * inconsistent state when one statement fails.
		 */
		PER_STATEMENT

	}

	/**
	 * This class has been introduced in 2.8.3 to configure the way version numbers are
	 * sorted. By default, they are sorted in lexicographic order ever since
	 * Neo4j-Migrations has been conceived. This has been an oversight and most likely not
	 * what is expected. We cannot change the default in the 2.x series, as that would be
	 * a possible hard breaking change, but 3.x will default to semantic ordering.
	 *
	 * @since 2.9.0
	 */
	public enum VersionSortOrder {

		/**
		 * Sort version numbers in lexicographic order (was the default in 1.x and 2.x).
		 */
		LEXICOGRAPHIC,

		/**
		 * Sort version numbers in semantic order (default since 3.0.0).
		 */
		SEMANTIC

	}

	/**
	 * This type has been introduced in 2.19.0 to allow configuration of the Cypher
	 * version in which all scripts should be run without the necessity to define this in
	 * individual scripts. This is especially helpful on databases that default to Cypher
	 * 25 and cannot run old scripts anymore. If you were to change the scripts,
	 * migrations would fail as the hash of the script would change, too. <br/>
	 * For now, this setting will only be applied to Cypher based resources and not to
	 * Java or XML based migrations. <br/>
	 * Read more about Cypher versioning <a href=
	 * "https://neo4j.com/docs/cypher-manual/current/queries/select-version/#select-default-cypher-version">here</a>.
	 */
	public enum CypherVersion {

		/**
		 * Uses the database defaults and will work with all versions of Neo4j.
		 */
		DATABASE_DEFAULT(""),
		/**
		 * Uses Cypher 5 and will work from later 5.26 versions.
		 */
		CYPHER_5("CYPHER 5"),
		/**
		 * Uses Cypher 25 and will work from 2025.06 onwards.
		 */
		CYPHER_25("CYPHER 25");

		private final String prefix;

		CypherVersion(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * {@return the prefix of this cypher version}
		 */
		public String getPrefix() {
			return this.prefix;
		}

	}

	/**
	 * A builder to create new instances of {@link MigrationsConfig configurations}.
	 */
	public static final class Builder {

		private String[] packagesToScan;

		private String[] locationsToScan;

		private TransactionMode transactionMode;

		private String database;

		private String impersonatedUser;

		private String installedBy;

		private boolean validateOnMigrate = Defaults.VALIDATE_ON_MIGRATE;

		private boolean autocrlf = Defaults.AUTOCRLF;

		private String schemaDatabase;

		private Discoverer<JavaBasedMigration> migrationClassesDiscoverer;

		private ClasspathResourceScanner resourceScanner;

		private Duration delayBetweenMigrations;

		private List<? extends RenderConfig.AdditionalRenderingOptions> constraintOptions = List.of();

		private VersionSortOrder versionSortOrder = Defaults.VERSION_SORT_ORDER;

		private Duration transactionTimeout;

		private boolean outOfOrder = Defaults.OUT_OF_ORDER;

		private String target;

		private boolean useFlywayCompatibleChecksums = Defaults.USE_FLYWAY_COMPATIBLE_CHECKSUMS;

		private CypherVersion cypherVersion = Defaults.CYPHER_VERSION;

		private Builder() {
			// The explicit constructor has been added to avoid warnings when
			// Neo4j-Migrations
			// is used on the module path. JMS will complain about Builder being exported
			// with
			// a public visible, implicit constructor.
		}

		/**
		 * Configures the list of packages to scan. Default is an empty list.
		 * @param packages one or more packages to scan.
		 * @return the builder for further customization
		 */
		public Builder withPackagesToScan(String... packages) {

			this.packagesToScan = packages;
			return this;
		}

		/**
		 * Configures the list of locations to scan. Defaults to a single entry of
		 * `classpath:neo4j/migrations`.
		 * @param locations one or more locations to scan. can start either with
		 * `classpath:` or `file:`. Locations without prefix are treated as classpath
		 * resources.
		 * @return the builder for further customization
		 */
		public Builder withLocationsToScan(String... locations) {

			this.locationsToScan = locations;
			return this;
		}

		/**
		 * Configures the transaction mode. Please have a look at {@link TransactionMode}
		 * regarding advantages and disadvantages of each mode.
		 * @param newTransactionMode the new transaction mode.
		 * @return the builder for further customization
		 */
		public Builder withTransactionMode(TransactionMode newTransactionMode) {

			this.transactionMode = newTransactionMode;
			return this;
		}

		/**
		 * Configures the database to apply Cypher-based migrations too. Leave null for
		 * the default database.
		 * @param newDatabase the database to use
		 * @return the builder for further customization
		 */
		public Builder withDatabase(String newDatabase) {

			this.database = newDatabase;
			return this;
		}

		/**
		 * Configures the user / principal name of the that is recorded in the MIGRATED_TO
		 * relationship as {@code by}. Defaults to the OS user.
		 * @param newInstalledBy an arbitrary string to represent the service having
		 * installed the migrations
		 * @return the builder for further customization
		 * @since 0.0.6
		 */
		public Builder withInstalledBy(String newInstalledBy) {

			this.installedBy = newInstalledBy;
			return this;
		}

		/**
		 * Validating helps you verify that the migrations applied to the database match
		 * the ones available locally and is on by default. It can be turned off by using
		 * a configuration with {@link MigrationsConfig#isValidateOnMigrate()} to
		 * {@literal false}.
		 * @param newValidateOnMigrate the new value for {@code validateOnMigrate}.
		 * @return the builder for further customization
		 * @since 0.2.1
		 */
		public Builder withValidateOnMigrate(boolean newValidateOnMigrate) {

			this.validateOnMigrate = newValidateOnMigrate;
			return this;
		}

		/**
		 * If you're programming on Windows and working with people who are not (or
		 * vice-versa), you'll probably run into line-ending issues at some point. This is
		 * because Windows uses both a carriage-return character and a linefeed character
		 * for newlines in its files, whereas macOS and Linux systems use only the
		 * linefeed character. This is a subtle but incredibly annoying fact of
		 * cross-platform work; many editors on Windows silently replace existing LF-style
		 * line endings with CRLF, or insert both line-ending characters when the user
		 * hits the enter key. Neo4j migrations can handle this by auto-converting CRLF
		 * line endings into LF before computing checksums of a Cypher-based migration or
		 * applying it.
		 * @param newAutocrlf the new value for {@code autocrlf}.
		 * @return the builder for further customization
		 * @since 0.3.1
		 */
		public Builder withAutocrlf(boolean newAutocrlf) {

			this.autocrlf = newAutocrlf;
			return this;
		}

		/**
		 * Configures the impersonated user to use. This works only with Neo4j 4.4+
		 * Enterprise and a Neo4j 4.4+ driver. The feature comes in handy for escalating
		 * privileges during the time of migrations and dropping them again for further
		 * use of a connection.
		 * @param newImpersonatedUser a user to impersonate
		 * @return the builder for further customization
		 * @since 1.0.0
		 */
		public Builder withImpersonatedUser(String newImpersonatedUser) {

			this.impersonatedUser = newImpersonatedUser;
			return this;
		}

		/**
		 * Configures the schema database to use. This is different from
		 * {@link #withDatabase(String)}. The latter configures the database that should
		 * be migrated, the schema database configures the database that holds the
		 * migration chain
		 * <p>
		 * To use this, Neo4j 4+ enterprise edition is required.
		 * @param newSchemaDatabase the new schema database to use.
		 * @return the builder for further customization
		 * @since 1.1.0
		 */
		public Builder withSchemaDatabase(String newSchemaDatabase) {

			this.schemaDatabase = newSchemaDatabase;
			return this;
		}

		/**
		 * Starts building a configuration by configuring the discoveres being used.
		 * @param newMigrationClassesDiscoverer the discoverer for (Java) class based
		 * migrations. Set to {@literal null} to use the default.
		 * @return the builder for further customization
		 * @since 1.3.0
		 */
		public Builder withMigrationClassesDiscoverer(Discoverer<JavaBasedMigration> newMigrationClassesDiscoverer) {

			this.migrationClassesDiscoverer = newMigrationClassesDiscoverer;
			return this;
		}

		/**
		 * Starts building a configuration by configuring the resource scanner being used.
		 * @param newResourceScanner the cypher resource scanner to be used. Set to
		 * {@literal null} to use the default.
		 * @return the builder for further customization
		 * @since 1.3.0
		 */
		public Builder withResourceScanner(ClasspathResourceScanner newResourceScanner) {

			this.resourceScanner = newResourceScanner;
			return this;
		}

		/**
		 * Some migrations, for example creating large indexes followed by large updates
		 * can cause an exception boiling down to {code Could not compile query due to
		 * insanely frequent schema changes}. To prevent this, you can configure a static
		 * delay between migrations.
		 * @param newDelayBetweenMigrations the delay to use. Use {@literal null} to
		 * disable delay (which is the default)
		 * @return the builder for further customization
		 * @since 2.3.2
		 */
		public Builder withDelayBetweenMigrations(Duration newDelayBetweenMigrations) {

			this.delayBetweenMigrations = newDelayBetweenMigrations;
			return this;
		}

		/**
		 * Configures the rendering options for constraints defined by a catalog. Can be
		 * {@literal null} but must not contain any {@literal null} items.
		 * @param newRenderingOptions the rendering options to use, a {@literal null}
		 * argument resets the options.
		 * @return the builder for further customization
		 * @throws NullPointerException if {@code newRenderingOptions} contains
		 * {@literal null} values
		 * @since 2.8.2
		 */
		public Builder withConstraintRenderingOptions(
				Collection<? extends RenderConfig.AdditionalRenderingOptions> newRenderingOptions) {

			this.constraintOptions = List.copyOf(Objects.requireNonNullElseGet(newRenderingOptions, List::of));
			return this;
		}

		/**
		 * Configures how versions are sorted.
		 * @param newVersionSortOrder the new order
		 * @return the builder for further customization
		 * @since 2.9.0
		 * @see VersionSortOrder
		 */
		public Builder withVersionSortOrder(VersionSortOrder newVersionSortOrder) {

			this.versionSortOrder = newVersionSortOrder;
			return this;
		}

		/**
		 * Configures the transaction timeout. Leave {@literal null} (the default), to use
		 * the drivers default
		 * @param newTransactionTimeout the transaction timeout
		 * @return the builder for further customization
		 * @since 2.13.0
		 */
		public Builder withTransactionTimeout(Duration newTransactionTimeout) {
			this.transactionTimeout = newTransactionTimeout;
			return this;
		}

		/**
		 * Allows or disallows migrations discovered to be out of order.
		 * @param allowed use {@literal true} to allow out-of-order discovery of
		 * migrations
		 * @return the builder for further customization
		 * @since 2.14.0
		 */
		public Builder withOutOfOrderAllowed(boolean allowed) {
			this.outOfOrder = allowed;
			return this;
		}

		/**
		 * Configures the target version up to which migrations should be considered. This
		 * must be a valid migration version, or one of the special values
		 * {@code current}, {@code latest} or {@code next}.
		 * @param newTarget the new target version
		 * @return the builder for further customization
		 * @since 2.15.0
		 */
		public Builder withTarget(String newTarget) {
			this.target = newTarget;
			return this;
		}

		/**
		 * Enables or disables migrations with checksums in Flyway compatible mode.
		 * @param enabled use {@literal true} to enable flyway compatible checksums
		 * @return the builder for further customization
		 * @since 2.17.0
		 */
		public Builder withFlywayCompatibleChecksums(boolean enabled) {
			this.useFlywayCompatibleChecksums = enabled;
			return this;
		}

		/**
		 * Selects the default cypher version. A {@literal null} argument uses
		 * {@link Defaults#CYPHER_VERSION}.
		 * @param newCypherVersion the cypher version to use as prefix for all cypher
		 * baseed migrations
		 * @return the builder for further customization
		 * @since 2.19.0
		 */
		public Builder withCypherVersion(CypherVersion newCypherVersion) {
			this.cypherVersion = newCypherVersion;
			return this;
		}

		/**
		 * {@return the immutable configuration}
		 */
		public MigrationsConfig build() {

			return new MigrationsConfig(this);
		}

	}

}
