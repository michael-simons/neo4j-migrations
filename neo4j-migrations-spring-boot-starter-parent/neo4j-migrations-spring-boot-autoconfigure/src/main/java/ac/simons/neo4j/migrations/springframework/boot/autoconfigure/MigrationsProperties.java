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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

import java.nio.charset.Charset;

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
	private String[] locationsToScan = Defaults.LOCATIONS_TO_SCAN.toArray(new String[0]);

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
	 * If you’re programming on Windows and working with people who are not (or vice-versa), you’ll probably run into
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
}
