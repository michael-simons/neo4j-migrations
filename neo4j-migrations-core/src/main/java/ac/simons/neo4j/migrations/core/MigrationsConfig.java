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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.internal.Strings;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	/**
	 * Used for configuring the transaction mode in Cypher-based transactions.
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

	private final Discoverer<JavaBasedMigration> migrationClassesDiscoverer;

	private final ClasspathResourceScanner resourceScanner;

	/**
	 * A configurable delay that will be applied in between applying two migrations.
	 */
	private final Duration delayBetweenMigrations;

	private List<? extends RenderConfig.AdditionalRenderingOptions> constraintOptions;

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
		this.migrationClassesDiscoverer = builder.migrationClassesDiscoverer == null ? new JavaBasedMigrationDiscoverer() : builder.migrationClassesDiscoverer;
		this.resourceScanner = builder.resourceScanner == null ? new DefaultClasspathResourceScanner() : builder.resourceScanner;
		this.schemaDatabase = builder.schemaDatabase;
		this.delayBetweenMigrations = builder.delayBetweenMigrations;
		this.constraintOptions = builder.constraintOptions;
	}

	/**
	 * @return the list of packages to scan
	 */
	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	/**
	 * @return the list of locations to scan
	 */
	public String[] getLocationsToScan() {
		return locationsToScan;
	}

	/**
	 * @return the transaction mode (whether to use one transaction for per migration or per statement)
	 */
	public TransactionMode getTransactionMode() {
		return transactionMode;
	}

	/**
	 * @return An optional target database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalDatabase() {
		return Strings.optionalOf(database);
	}

	/**
	 * @return An optional schema database
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalSchemaDatabase() {
		return Strings.optionalOf(schemaDatabase);
	}

	/**
	 * @return An optional user to impersonate
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalImpersonatedUser() {
		return Strings.optionalOf(impersonatedUser);
	}

	/**
	 * @return Optional user information about the user executing the migration
	 * @since 1.1.0
	 */
	public Optional<String> getOptionalInstalledBy() {
		return Strings.optionalOf(installedBy);
	}

	/**
	 * @return {@literal true} if resolved migrations and database state should be validated before a migration attempt is applied
	 */
	public boolean isValidateOnMigrate() {
		return validateOnMigrate;
	}

	/**
	 * @return whether {@literal CRLF} line endings should be automatically converted into {@literal LF}
	 */
	public boolean isAutocrlf() {
		return autocrlf;
	}

	/**
	 * @return The discoverer for class based migrations,  never {@literal null}
	 * @since 1.3.0
	 */
	public Discoverer<JavaBasedMigration> getMigrationClassesDiscoverer() {
		return migrationClassesDiscoverer;
	}

	/**
	 * @return The resource scanner, never {@literal null}
	 * @since 1.3.0
	 */
	public ClasspathResourceScanner getResourceScanner() {
		return resourceScanner;
	}

	/**
	 * @return The delay to apply between migrations
	 * @since 2.3.2
	 */
	public Optional<Duration> getOptionalDelayBetweenMigrations() {
		return Optional.ofNullable(delayBetweenMigrations);
	}

	/**
	 * {@return the list of additional options to use when rendering constraints}
	 * @since 2.8.2
	 */
	public List<? extends RenderConfig.AdditionalRenderingOptions> getConstraintRenderingOptions() {
		return List.copyOf(constraintOptions);
	}

	/**
	 * Helper method to pretty print this configuration into a logger (on level {@literal INFO} respectively {@literal WARNING}.
	 *
	 * @param logger  the logger to print to
	 * @param verbose set to {@literal true} if you want to print all details
	 */
	public void logTo(Logger logger, boolean verbose) {
		if (!this.hasPlacesToLookForMigrations()) {
			logger.log(Level.WARNING, "Cannot find migrations as neither locations nor packages to scan are configured!");
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
	 */
	boolean hasPlacesToLookForMigrations() {
		return this.getPackagesToScan().length > 0 || this.getLocationsToScan().length > 0;
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

		if (optionalSchemaDatabase.isEmpty()) {
			return Optional.empty();
		}

		if (optionalDatabase.isEmpty()) {
			// We need to connect to get this information
			try (Session session = context.getSession()) {
				try {
					optionalDatabase = Optional.of(session.executeRead(tx -> tx.run("CALL db.info() YIELD name").single().get("name").asString()));
				} catch (ClientException e) {
					optionalDatabase =
						Optional.ofNullable(session.executeRead(tx -> tx.run("MATCH (n) RETURN count(n)").consume()).database())
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

		private Discoverer<JavaBasedMigration> migrationClassesDiscoverer;

		private ClasspathResourceScanner resourceScanner;

		private Duration delayBetweenMigrations;

		private List<? extends RenderConfig.AdditionalRenderingOptions> constraintOptions = List.of();

		private Builder() {
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
		 * Configures the database to apply Cypher-based migrations too. Leave null for the default database.
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
		 * If you're programming on Windows and working with people who are not (or vice-versa), you'll probably run into
		 * line-ending issues at some point. This is because Windows uses both a carriage-return character and a linefeed
		 * character for newlines in its files, whereas macOS and Linux systems use only the linefeed character.
		 * This is a subtle but incredibly annoying fact of cross-platform work; many editors on Windows silently replace
		 * existing LF-style line endings with CRLF, or insert both line-ending characters when the user hits the enter key.
		 * Neo4j migrations can handle this by auto-converting CRLF line endings into LF before computing checksums of a
		 * Cypher-based migration or applying it.
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
		 * @param newMigrationClassesDiscoverer The discoverer for (Java) class based migrations. Set to {@literal null} to use the default.
		 * @return The builder for further customization
		 * @since 1.3.0
		 */
		public Builder withMigrationClassesDiscoverer(Discoverer<JavaBasedMigration> newMigrationClassesDiscoverer) {

			this.migrationClassesDiscoverer = newMigrationClassesDiscoverer;
			return this;
		}

		/**
		 * @param newResourceScanner The Cypher resource scanner to be used. Set to {@literal null} to use the default.
		 * @return The builder for further customization
		 * @since 1.3.0
		 */
		public Builder withResourceScanner(ClasspathResourceScanner newResourceScanner) {

			this.resourceScanner = newResourceScanner;
			return this;
		}

		/**
		 * Some migrations, for example creating large indexes followed by large updates can cause an exception boiling down to
		 * {code Could not compile query due to insanely frequent schema changes}. To prevent this, you can configure a static
		 * delay between migrations.
		 *
		 * @param newDelayBetweenMigrations The delay to use. Use {@literal null} to disable delay (which is the default)
		 * @return The builder for further customization
		 * @since 2.3.2
		 */
		public Builder withDelayBetweenMigrations(Duration newDelayBetweenMigrations) {

			this.delayBetweenMigrations = newDelayBetweenMigrations;
			return this;
		}

		/**
		 * Configures the rendering options for constraints defined by a catalog. Can be {@literal null} but must not
		 * contain any {@literal null} items.
		 * @param newRenderingOptions The rendering options to use, a {@literal null} argument resets the options.
		 * @return The builder for further customization
		 * @throws NullPointerException if {@code newRenderingOptions} contains {@literal null} values
		 * @since 2.8.2
		 */
		public Builder withConstraintRenderingOptions(Collection<? extends RenderConfig.AdditionalRenderingOptions> newRenderingOptions) {

			this.constraintOptions = List.copyOf(Objects.requireNonNullElseGet(newRenderingOptions, List::of));
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
