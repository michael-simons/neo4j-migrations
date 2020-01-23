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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

import java.nio.charset.Charset;
import java.util.List;

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
	private String[] locationsToScan = Defaults.LOCATIONS_TO_SCAN;

	/**
	 * The transaction mode in use (Defaults to "per migration", meaning one script is run in one transaction).
	 */
	private TransactionMode transactionMode = Defaults.TRANSACTION_MODE;

	/**
	 * Encoding of Cypher migrations.
	 */
	private Charset encoding = Defaults.CYPHER_SCRIPT_ENCODING;

	/**
	 * The database that should be migrated (Neo4j 4.0+ only). Leave {@literal null} for using the default database.
	 */
	private String database;

	/**
	 * Username recorded as property {@literal by} on the MIGRATED_TO relationship.
	 */
	private String installedBy;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCheckLocation() {
		return checkLocation;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public void setPackagesToScan(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	public String[] getLocationsToScan() {
		return locationsToScan;
	}

	public void setLocationsToScan(String[] locationsToScan) {
		this.locationsToScan = locationsToScan;
	}

	public String getInstalledBy() {
		return installedBy;
	}

	public void setInstalledBy(String installedBy) {
		this.installedBy = installedBy;
	}

	public TransactionMode getTransactionMode() {
		return transactionMode;
	}

	public void setTransactionMode(TransactionMode transactionMode) {
		this.transactionMode = transactionMode;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}
}
