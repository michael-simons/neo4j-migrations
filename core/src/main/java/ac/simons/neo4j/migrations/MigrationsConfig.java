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
package ac.simons.neo4j.migrations;

import java.util.Optional;

/**
 * Configuration for Migrations.
 *
 * @author Michael J. Simons
 */
public final class MigrationsConfig {

	static final String PREFIX_FILESYSTEM = "filesystem";
	static final String PREFIX_CLASSPATH = "classpath";

	/**
	 * Used for configuring the transaction mode in Cypher based transactions.
	 */
	public enum TransactionMode {

		/**
		 * Run all statements in one transactions. May need more memory, but it's generally safer. Either the
		 * migration runs as a whole or not not at all.
		 */
		PER_MIGRATION,
		/**
		 * Runs each statement in a separate transaction. May leave your database in an inconsistent state when
		 * one statement fails.
		 */
		PER_STATEMENT
	}

	public static Builder builder() {

		return new Builder();
	}

	static MigrationsConfig defaultConfig() {

		return builder().build();
	}

	private final String[] packagesToScan;

	private final String[] locationsToScan;

	private final TransactionMode transactionMode;

	private final String database;

	private MigrationsConfig(Builder builder) {

		this.packagesToScan = builder.packagesToScan == null ? Defaults.PACKAGES_TO_SCAN : builder.packagesToScan;
		this.locationsToScan =
			builder.locationsToScan == null ? Defaults.LOCATIONS_TO_SCAN : builder.locationsToScan;
		this.transactionMode = Optional.ofNullable(builder.transactionMode).orElse(TransactionMode.PER_MIGRATION);
		this.database = builder.database;
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

	public String getDatabase() {
		return database;
	}

	public static class Builder {

		private String[] packagesToScan;

		private String[] locationsToScan;

		private TransactionMode transactionMode;

		private String database;

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
		 * @param locations one or more locations to scan. Can start either with `classpath:` or `filesystem:`. Locations
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

		public MigrationsConfig build() {

			return new MigrationsConfig(this);
		}
	}
}
