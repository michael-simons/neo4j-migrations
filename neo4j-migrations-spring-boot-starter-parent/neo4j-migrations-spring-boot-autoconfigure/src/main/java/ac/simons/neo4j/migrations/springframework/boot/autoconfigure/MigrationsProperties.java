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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsConfig.CypherVersion;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsConfig.VersionSortOrder;

import java.nio.charset.Charset;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for a migration instance.
 *
 * @author Michael J. Simons
 * @since 0.0.6
 */
@ConfigurationProperties(prefix = "org.neo4j.migrations")
public class MigrationsProperties {

	/**
	 * Whether to enable Neo4j migrations or not.
	 */
	private boolean enabled = true;

	/**
	 * Whether to check that migration scripts location exists.
	 */
	private boolean checkLocation = true;

	/**
	 * List of packages to scan for Java migrations.
	 */
	private String[] packagesToScan = new String[0];

	/**
	 * Locations of migrations scripts.
	 */
	private String[] locationsToScan = new String[] { Defaults.LOCATIONS_TO_SCAN_VALUE };

	/**
	 * The transaction mode in use (Defaults to "per migration", meaning one script is run in one transaction).
	 */
	private TransactionMode transactionMode = Defaults.TRANSACTION_MODE;

	/**
	 * Encoding of Cypher migrations.
	 */
	private Charset encoding = Defaults.CYPHER_SCRIPT_ENCODING;

	/**
	 * The database that should be migrated (Neo4j EE 4.0+ only). Leave {@literal null} for using the default database.
	 */
	private String database;

	/**
	 * The database that should be used for storing informations about migrations (Neo4j EE 4.0+ only). Leave {@literal null} for using the default database.
	 */
	private String schemaDatabase;

	/**
	 * An alternative user to impersonate during migration. Might have higher privileges than the user connected, which
	 * will be dropped again after migration. Requires Neo4j EE 4.4+. Leave {@literal null} for using the connected user.
	 */
	private String impersonatedUser;

	/**
	 * Username recorded as property {@literal by} on the MIGRATED_TO relationship.
	 */
	private String installedBy;

	/**
	 * Validating helps you verify that the migrations applied to the database match the ones available locally and is on by default.
	 */
	private boolean validateOnMigrate = Defaults.VALIDATE_ON_MIGRATE;

	/**
	 * If you're programming on Windows and working with people who are not (or vice-versa), you'll probably run into
	 * line-ending issues at some point. This is because Windows uses both a carriage-return character and a linefeed
	 * character for newlines in its files, whereas macOS and Linux systems use only the linefeed character.
	 * This is a subtle but incredibly annoying fact of cross-platform work; many editors on Windows silently replace
	 * existing LF-style line endings with CRLF, or insert both line-ending characters when the user hits the enter key.
	 *
	 * Neo4j migrations can handle this by auto-converting CRLF line endings into LF before computing checksums of a
	 * Cypher-based migration or applying it.
	 */
	private boolean autocrlf = Defaults.AUTOCRLF;

	/**
	 * A configurable delay that will be applied in between applying two migrations.
	 *
	 * @since 2.3.2
	 */
	private Duration delayBetweenMigrations;

	/**
	 * Configures the sort order for migrations.
	 *
	 * @see VersionSortOrder
	 * @since 2.9.0
	 */
	private VersionSortOrder versionSortOrder = Defaults.VERSION_SORT_ORDER;

	/**
	 * Configures the transaction timeout that should be applied for each migration or each statement (the latter depends on {@link #transactionMode}).
	 * {@literal null} is a valid value and make the driver apply the default timeout for the database.
	 *
	 * @since 2.13.0
	 */
	private Duration transactionTimeout;

	/**
	 * When this flag is set to {@literal true}, new migrations discovered that are "out of order", such as a version 15
	 * is to be found between 10 and 20, it will be accepted, integrated into the chain and then move on instead of throwing
	 * an error.
	 *
	 * @since 2.14.0
	 */
	private boolean outOfOrder = Defaults.OUT_OF_ORDER;

	/**
	 * When this flag is set to {@literal true}, checksums will be computed with the same algorithm Flyway uses.
	 *
	 * @since 2.17.0
	 */
	private boolean useFlywayCompatibleChecksums = Defaults.USE_FLYWAY_COMPATIBLE_CHECKSUMS;

	/**
	 * Use this property to configure a Cypher version that will be prepended to every statement in every migration found.
	 * Leave it {@literal null} or use {@link CypherVersion#DATABASE_DEFAULT} (the default), the keep the existing behaviour
	 * of letting the database decide.
	 *
	 * @since 2.19.0
	 */
	private CypherVersion cypherVersion = Defaults.CYPHER_VERSION;

	/**
	 * Configures the target version up to which migrations should be considered. This must be a valid migration version,
	 * or one of the special values {@code current}, {@code latest} or {@code next}.
	 *
	 * @since 2.15.0
	 */
	private String target;

	/**
	 * @return see {@link #enabled}
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled A new value for {@link #enabled}
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return see {@link #checkLocation}
	 */
	public boolean isCheckLocation() {
		return checkLocation;
	}

	/**
	 * @param checkLocation A new value for {@link #checkLocation}
	 */
	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	/**
	 * @return see {@link #packagesToScan}
	 */
	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	/**
	 * @param packagesToScan A new value for {@link #packagesToScan}
	 */
	public void setPackagesToScan(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * @return see {@link #locationsToScan}
	 */
	public String[] getLocationsToScan() {
		return locationsToScan;
	}

	/**
	 * @param locationsToScan A new value for {@link #locationsToScan}
	 */
	public void setLocationsToScan(String[] locationsToScan) {
		this.locationsToScan = locationsToScan;
	}

	/**
	 * @return see {@link #installedBy}
	 */
	public String getInstalledBy() {
		return installedBy;
	}

	/**
	 * @param installedBy A new value for {@link #installedBy}
	 */
	public void setInstalledBy(String installedBy) {
		this.installedBy = installedBy;
	}

	/**
	 * @return see {@link #transactionMode}
	 */
	public TransactionMode getTransactionMode() {
		return transactionMode;
	}

	/**
	 * @param transactionMode A new value for {@link #transactionMode}
	 */
	public void setTransactionMode(TransactionMode transactionMode) {
		this.transactionMode = transactionMode;
	}

	/**
	 * @return see {@link #encoding}
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding A new value for {@link #encoding}
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * @return see {@link #database}
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * @param database A new value for {@link #database}
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * @return see {@link #schemaDatabase}
	 */
	public String getSchemaDatabase() {
		return schemaDatabase;
	}

	/**
	 * @param schemaDatabase A new value for {@link #schemaDatabase}
	 */
	public void setSchemaDatabase(String schemaDatabase) {
		this.schemaDatabase = schemaDatabase;
	}

	/**
	 * @return see {@link #impersonatedUser}
	 */
	public String getImpersonatedUser() {
		return impersonatedUser;
	}

	/**
	 * @param impersonatedUser A new value for {@link #impersonatedUser}
	 */
	public void setImpersonatedUser(String impersonatedUser) {
		this.impersonatedUser = impersonatedUser;
	}

	/**
	 * @return see {@link #validateOnMigrate}
	 */
	public boolean isValidateOnMigrate() {
		return validateOnMigrate;
	}

	/**
	 * @param validateOnMigrate A new value for {@link #validateOnMigrate}
	 */
	public void setValidateOnMigrate(boolean validateOnMigrate) {
		this.validateOnMigrate = validateOnMigrate;
	}

	/**
	 * @return see {@link #autocrlf}
	 */
	public boolean isAutocrlf() {
		return autocrlf;
	}

	/**
	 * @param autocrlf A new value for {@link #autocrlf}
	 */
	public void setAutocrlf(boolean autocrlf) {
		this.autocrlf = autocrlf;
	}

	/**
	 * @return see {@link #delayBetweenMigrations}
	 */
	public Duration getDelayBetweenMigrations() {
		return delayBetweenMigrations;
	}

	/**
	 * @param delayBetweenMigrations A new value for {@link #delayBetweenMigrations}
	 */
	public void setDelayBetweenMigrations(Duration delayBetweenMigrations) {
		this.delayBetweenMigrations = delayBetweenMigrations;
	}

	/**
	 * @return see {@link #versionSortOrder}
	 */
	public VersionSortOrder getVersionSortOrder() {
		return versionSortOrder;
	}

	/**
	 * @param versionSortOrder A new value for {@link #versionSortOrder}
	 */
	public void setVersionSortOrder(VersionSortOrder versionSortOrder) {
		this.versionSortOrder = versionSortOrder;
	}

	/**
	 * @return see {@link #transactionTimeout}
	 */
	public Duration getTransactionTimeout() {
		return transactionTimeout;
	}

	/**
	 * @param transactionTimeout A new value for {@link #transactionTimeout}
	 */
	public void setTransactionTimeout(Duration transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * {@return whether out-of-order migrations are allowed and integrated into the chain or not}
	 */
	public boolean isOutOfOrder() {
		return outOfOrder;
	}

	/**
	 * {@return whether to use Flyway compatible checksums or not}
	 * @since 2.17.0
	 */
	public boolean isUseFlywayCompatibleChecksums() {
		return useFlywayCompatibleChecksums;
	}

	/**
	 * Configures whether Flyway compatible checksums should be used or not
	 * @param useFlywayCompatibleChecksums the new value for the corresponding flag
	 * @since 2.17.0
	 */
	public void setUseFlywayCompatibleChecksums(boolean useFlywayCompatibleChecksums) {
		this.useFlywayCompatibleChecksums = useFlywayCompatibleChecksums;
	}

	/**
	 * Configures whether migrations are allowed to be out-of-order or not
	 * @param outOfOrder the new value for the out-of-order flag
	 */
	public void setOutOfOrder(boolean outOfOrder) {
		this.outOfOrder = outOfOrder;
	}

	/**
	 * {@return the version up-to which migrations should be considered}
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Configures the target version to consider.
	 * @param target the new value for the target version
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * {@return the Cypher version to use for statements in Cypher based migrations}
	 */
	public CypherVersion getCypherVersion() {
		return cypherVersion;
	}

	/**
	 * Configures the Cypher version to use for statements in Cypher based migrations.
	 * @param cypherVersion set to {@literal null} or {@link CypherVersion#DATABASE_DEFAULT} to use the database default
	 */
	public void setCypherVersion(CypherVersion cypherVersion) {
		this.cypherVersion = cypherVersion;
	}
}
