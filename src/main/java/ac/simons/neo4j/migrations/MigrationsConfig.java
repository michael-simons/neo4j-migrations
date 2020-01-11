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

/**
 * Configuration for Migrations.
 *
 * @author Michael J. Simons
 */
public final class MigrationsConfig {

	static final String PREFIX_FILESYSTEM = "filesystem";
	static final String PREFIX_CLASSPATH = "classpath";

	public static Builder builder() {

		return new Builder();
	}

	private final String[] packagesToScan;

	private final String[] locationsToScan;

	private MigrationsConfig(Builder builder) {

		this.packagesToScan = builder.packagesToScan == null ? Defaults.PACKAGES_TO_SCAN : builder.packagesToScan;
		this.locationsToScan =
			builder.locationsToScan == null ? Defaults.LOCATIONS_TO_SCAN : builder.locationsToScan;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public String[] getLocationsToScan() {
		return locationsToScan;
	}

	public static class Builder {

		private String[] packagesToScan;

		private String[] locationsToScan;

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

		public MigrationsConfig build() {

			return new MigrationsConfig(this);
		}
	}
}
