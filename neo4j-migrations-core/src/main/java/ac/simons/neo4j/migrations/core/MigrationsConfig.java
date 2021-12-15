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
package ac.simons.neo4j.migrations.core;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.Session;
import org.neo4j.driver.summary.DatabaseInfo;

/**
 * Configuration for Migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class MigrationsConfig {

	static final String PREFIX_FILESYSTEM = "file";
	static final String PREFIX_CLASSPATH = "classpath";

	/**
	 * Used for configuring the transaction mode in Cypher based transactions.
	 */
	public enum TransactionMode {

		/**
		 * Run all statements in one transaction. May need more memory, but it's generally safer. Either the
		 * migration runs as a whole or not at all.
		 */
		PER_MIGRATION,
		/**
		 * Runs each statement in a separate transaction. May leave your database in an inconsistent state when
		 * one statement fails.
		 */
		PER_STATEMENT
	}

	/**
	 * Start building a new configuration.
	 *
	 * @return The entry point for creating a new configuration.
	 * @since 0.0.1
	 */
	@SuppressWarnings("squid:S1874") // Remove it as soon as the constructor is made private and no longer marked as deprecated.
	public static Builder builder() {

		return new Builder();
	}

	/**
	 * @return The default config
	 * @since 0.0.6
	 */
	public static MigrationsConfig defaultConfig() {

		return builder().build();
	}

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

	private MigrationsConfig(Builder builder) {

		this.packagesToScan =
			builder.packagesToScan == null ? Defaults.PACKAGES_TO_SCAN.toArray(new String[0]) : builder.packagesToScan;
		this.locationsToScan =
			builder.locationsToScan == null ?
				Defaults.LOCATIONS_TO_SCAN.toArray(new String[0]) :
				builder.locationsToScan;
		this.transactionMode = Optional.ofNullable(builder.transactionMode).orElse(TransactionMode.PER_MIGRATION);
		this.database = builder.database;
		this.impersonatedUser = builder.impersonatedUser;
		this.installedBy = Optional.ofNullable(builder.installedBy).orElse(System.getProperty("user.name"));
		this.validateOnMigrate = builder.validateOnMigrate;
		this.autocrlf = builder.autocrlf;
		this.schemaDatabase = builder.schemaDatabase;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public String[] getLocationsToScan() {
		return locationsToScan;
	}

	public TransactionMode getTransactionMode() {
		return transactionMode;
	}

	private static boolean valueIsNotBlank(String value) {
		return !value.trim().isEmpty();
	}

	/**
	 * @return An optional target database, maybe {@literal null}
	 * @deprecated since 1.1.0, please use {@link #getOptionalDatabase()}
	 */
	@Deprecated
	public String getDatabase() {
		return database;
	}

	/**
	 * @return An optional target database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalDatabase() {
		return optionalOf(database);
	}

	/**
	 * @return An optional schema database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalSchemaDatabase() {
		return optionalOf(schemaDatabase);
	}

	/**
	 * @return An optional user to impersonate, maybe {@literal null}
	 * @deprecated since 1.1.0, please use {@link #getOptionalImpersonatedUser()}
	 */
	@Deprecated
	public String getImpersonatedUser() {
		return impersonatedUser;
	}

	/**
	 * @return An optional user to impersonate
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalImpersonatedUser() {
		return optionalOf(impersonatedUser);
	}

	/**
	 * @return Optional user information about the user executing the migration, maybe {@literal null}
	 * @deprecated since 1.1.0, please use {@link #getOptionalInstalledBy()}
	 */
	@Deprecated
	public String getInstalledBy() {
		return installedBy;
	}

	/**
	 * @return Optional user information about the user executing the migration
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalInstalledBy() {
		return optionalOf(installedBy);
	}

	public boolean isValidateOnMigrate() {
		return validateOnMigrate;
	}

	public boolean isAutocrlf() {
		return autocrlf;
	}

	public void logTo(Logger logger, boolean verbose) {
		if (!this.hasPlacesToLookForMigrations()) {
			logger.log(Level.WARNING, "Cannot find migrations as neither locations or packages to scan are configured!");
		}

		if (verbose && logger.isLoggable(Level.INFO)) {
			this.getOptionalDatabase().ifPresent(v -> logger.log(Level.INFO, "Migrations will be applied to database \"{0}\"", v));
			if (this.getLocationsToScan().length > 0) {
				logger.log(Level.INFO, "Will search for Cypher scripts in \"{0}\"",
					String.join("", this.getLocationsToScan()));
				logger.log(Level.INFO, "Statements will be applied {0}",
					this.getTransactionMode() == TransactionMode.PER_MIGRATION ?
						"in one transaction per migration" :
						"in separate transactions");
			}
			if (this.getPackagesToScan().length > 0) {
				logger.log(Level.INFO, "Will scan for Java-based migrations in \"{0}\"", String.join("", this.getPackagesToScan()));
			}
		}
	}

	/**
	 * This is internal API and will be made package private in 2.0.0
	 * @return True if there are packages to scan
	 * @deprecated Since 1.1.0, will be removed from public without replace.
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public boolean hasPlacesToLookForMigrations() {
		return this.getPackagesToScan().length > 0 || this.getLocationsToScan().length > 0;
	}

	Optional<String> optionalOf(String value) {
		return Optional.ofNullable(value)
			.filter(MigrationsConfig::valueIsNotBlank)
			.map(String::trim);
	}

	/**
	 * The migration target will be empty if no target database is given or the schema database is the same as the target.
	 *
	 * @param context The context in which the target should be retrieved
	 * @return A target database to use for all chains stored.
	 */
	Optional<String> getMigrationTargetIn(MigrationContext context) {
		Optional<String> optionalDatabase = getOptionalDatabase();
		Optional<String> optionalSchemaDatabase = getOptionalSchemaDatabase();

		if (!optionalSchemaDatabase.isPresent()) {
			return Optional.empty();
		}

		if (!optionalDatabase.isPresent()) {
			// We need to connect to get this information
			try (Session session = context.getSession()) {
				optionalDatabase =
					Optional.ofNullable(session.run("MATCH (n) RETURN count(n)").consume().database())
						.map(DatabaseInfo::name);
			}
		}

		if (!optionalDatabase.equals(optionalSchemaDatabase)) {
			return optionalDatabase.map(s -> s.toLowerCase(Locale.ROOT));
		}
		return Optional.empty();
	}

	/**
	 * A builder to create new instances of {@link MigrationsConfig configurations}.
	 */
	public static class Builder {

		private String[] packagesToScan;

		private String[] locationsToScan;

		private TransactionMode transactionMode;

		private String database;

		private String impersonatedUser;

		private String installedBy;

		private boolean validateOnMigrate = Defaults.VALIDATE_ON_MIGRATE;

		private boolean autocrlf = Defaults.AUTOCRLF;

		private String schemaDatabase;

		/**
		 * @deprecated since 1.1.0, will be made private in 2.0.0, please use {@link MigrationsConfig#builder()}.
		 */
		@SuppressWarnings({ "DeprecatedIsStillUsed", "squid:S1133" }) // The deprecation warning on any client code calling this is actually the point.
		@Deprecated
		public Builder() {
			// The explicit constructor has been added to avoid warnings when Neo4j-Migrations
			// is used on the module path. JMS will complain about Builder being exported with
			// a public visible, implicit constructor.
		}

		/**
		 * Configures the list of packages to scan. Default is an empty list.
		 *
		 * @param packages one or more packages to scan.
		 * @return The builder for further customization
		 */
		public Builder withPackagesToScan(String... packages) {

			this.packagesToScan = packages;
			return this;
		}

		/**
		 * Configures the list of locations to scan. Defaults to a single entry of `classpath:neo4j/migrations`.
		 *
		 * @param locations one or more locations to scan. Can start either with `classpath:` or `file:`. Locations
		 *                  without prefix are treated as classpath resources.
		 * @return The builder for further customization
		 */
		public Builder withLocationsToScan(String... locations) {

			this.locationsToScan = locations;
			return this;
		}

		/**
		 * Configures the transaction mode. Please have a look at {@link TransactionMode} regarding advantages and
		 * disadvantages of each mode.
		 *
		 * @param newTransactionMode The new transaction mode.
		 * @return The builder for further customization
		 */
		public Builder withTransactionMode(TransactionMode newTransactionMode) {

			this.transactionMode = newTransactionMode;
			return this;
		}

		/**
		 * Configures the database to apply cypher based migrations too. Leave null for the default database.
		 *
		 * @param newDatabase The database to use
		 * @return The builder for further customization
		 */
		public Builder withDatabase(String newDatabase) {

			this.database = newDatabase;
			return this;
		}

		/**
		 * Configures the user / principal name of the that is recorded in the MIGRATED_TO relationship as {@code by}.
		 * Defaults to the OS user.
		 *
		 * @param newInstalledBy An arbitrary string to represent the service having installed the migrations
		 * @return The builder for further customization
		 * @since 0.0.6
		 */
		public Builder withInstalledBy(String newInstalledBy) {

			this.installedBy = newInstalledBy;
			return this;
		}

		/**
		 * Validating helps you verify that the migrations applied to the database match the ones available locally and
		 * is on by default. It can be turned off by using a configuration with {@link MigrationsConfig#isValidateOnMigrate()}
		 * to {@literal false}.
		 *
		 * @param newValidateOnMigrate The new value for {@code validateOnMigrate}.
		 * @return The builder for further customization
		 * @since 0.2.1
		 */
		public Builder withValidateOnMigrate(boolean newValidateOnMigrate) {

			this.validateOnMigrate = newValidateOnMigrate;
			return this;
		}

		/**
		 * If you’re programming on Windows and working with people who are not (or vice-versa), you’ll probably run into
		 * line-ending issues at some point. This is because Windows uses both a carriage-return character and a linefeed
		 * character for newlines in its files, whereas macOS and Linux systems use only the linefeed character.
		 * This is a subtle but incredibly annoying fact of cross-platform work; many editors on Windows silently replace
		 * existing LF-style line endings with CRLF, or insert both line-ending characters when the user hits the enter key.
		 * Neo4j migrations can handle this by auto-converting CRLF line endings into LF before computing checksums of a
		 * Cypher based migration or applying it.
		 *
		 * @param newAutocrlf The new value for {@code autocrlf}.
		 * @return The builder for further customization
		 * @since 0.3.1
		 */
		public Builder withAutocrlf(boolean newAutocrlf) {

			this.autocrlf = newAutocrlf;
			return this;
		}

		/**
		 * Configures the impersonated user to use. This works only with Neo4j 4.4+ Enterprise and a Neo4j 4.4+ driver.
		 * The feature comes in handy for escalating privileges during the time of migrations and dropping them again
		 * for further use of a connection.
		 *
		 * @param newImpersonatedUser A user to impersonate
		 * @return The builder for further customization
		 * @since 1.0.0
		 */
		public Builder withImpersonatedUser(String newImpersonatedUser) {

			this.impersonatedUser = newImpersonatedUser;
			return this;
		}

		/**
		 * Configures the schema database to use. This is different from {@link #withDatabase(String)}. The latter configures
		 * the database that should be migrated, the schema database configures the database that holds the migration chain
		 * <p>
		 * To use this, Neo4j 4+ enterprise edition is required.
		 *
		 * @param newSchemaDatabase The new schema database to use.
		 * @return The builder for further customization
		 * @since 1.1.0
		 */
		public Builder withSchemaDatabase(String newSchemaDatabase) {

			this.schemaDatabase = newSchemaDatabase;
			return this;
		}

		/**
		 * @return The immutable configuration
		 */
		public MigrationsConfig build() {

			return new MigrationsConfig(this);
		}
	}
}
