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
package ac.simons.neo4j.migrations.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import ac.simons.neo4j.migrations.core.MigrationVersion.TargetVersion;

/**
 * Public information about an applied migration. All migrations (applied and pending)
 * form a chain of transformations to a database. The chain starts implicit with a
 * baseline version. The baseline version is not contained in this chain.
 *
 * @author Michael J. Simons
 * @since 0.0.4
 */
public sealed interface MigrationChain extends ConnectionDetails permits DefaultMigrationChain {

	/**
	 * Creates an empty instance of a {@link MigrationChain}. Can be useful for testing.
	 * @param connectionDetails required to initialize the chain
	 * @return an empty migration chain
	 * @since 2.3.0
	 */
	static MigrationChain empty(ConnectionDetails connectionDetails) {
		return new DefaultMigrationChain(connectionDetails, Map.of());
	}

	/**
	 * Pretty prints this chain as an ASCII table.
	 * @return a formatted string (an ASCII table representing the chain)
	 * @since 0.0.11
	 */
	default String prettyPrint() {

		StringBuilder sb = new StringBuilder(ConnectionDetailsFormatter.INSTANCE.format(this))
			.append(MigrationChainFormat.LS)
			.append(MigrationChainFormat.LS);

		if (getElements().isEmpty()) {
			sb.append(MigrationChainFormat.LS).append("No migrations found.");
		}
		else {
			MigrationChainFormat.formatElements(this, sb);
		}
		return sb.toString();
	}

	/**
	 * Returns true, if the version string maps to a migration that has been applied.
	 * @param version an arbitrary version string
	 * @return true, if the version string maps to a migration that has been applied.
	 */
	boolean isApplied(String version);

	/**
	 * {@return the elements of this migration}
	 */
	Collection<Element> getElements();

	/**
	 * {@return the length of this chain.}
	 * @since 2.2.0
	 */
	default int length() {
		return getElements().size();
	}

	/**
	 * {@return the last applied version}
	 * @since 1.11.0
	 */
	default Optional<MigrationVersion> getLastAppliedVersion() {
		return Optional.empty();
	}

	/**
	 * Translates a target version into a concrete version.
	 * @param targetVersion the target version to translate into a concrete version
	 * @return the concrete version if any
	 * @since 2.15.0
	 */
	Optional<MigrationVersion> findTargetVersion(TargetVersion targetVersion);

	/**
	 * Used for selecting how the {@link MigrationChain} should be computed.
	 *
	 * @since 1.4.0
	 * @see Migrations#info(ChainBuilderMode)
	 */
	enum ChainBuilderMode {

		/**
		 * Create the chain of applied and pending migrations by comparing the locally
		 * discovered migrations and the migrations applied remotely. Validation will be
		 * performed (such as checking for the correct number of migrations and checksum
		 * comparison).
		 */
		COMPARE,
		/**
		 * Build a chain only based on locally discovered migrations and assume all
		 * migrations are pending. No validation will be performed.
		 */
		LOCAL,
		/**
		 * Build a chain only based on remotely applied migrations and assume all
		 * migrations are applied. No validation will be performed.
		 */
		REMOTE

	}

	/**
	 * A chain element describing a pending or applied migration.
	 */
	sealed interface Element permits DefaultMigrationChainElement {

		/**
		 * {@return the state of this migration}
		 */
		MigrationState getState();

		/**
		 * {@return the type of the migration}
		 */
		MigrationType getType();

		/**
		 * {@return the checksum of this migration if available}
		 */
		Optional<String> getChecksum();

		/**
		 * {@return the schema version after the migration is complete}
		 */
		String getVersion();

		/**
		 * {@return an optional description of the migration represented by this element}
		 * @since 1.9.0
		 */
		Optional<String> getOptionalDescription();

		/**
		 * {@return the name of the script or class on which this migration is based}
		 */
		String getSource();

		/**
		 * {@return the timestamp when this migration was installed. (Only for applied
		 * migrations)}
		 */
		Optional<ZonedDateTime> getInstalledOn();

		/**
		 * {@return the user that installed this migration. (Only for applied migrations)}
		 */
		Optional<String> getInstalledBy();

		/**
		 * {@return the execution time of this migration. (Only for applied migrations)}
		 */
		Optional<Duration> getExecutionTime();

		/**
		 * Creates a map representation of this element. Non-present elements will be
		 * represented as empty strings.
		 * @return a map containing all present information in this element
		 * @since 2.3.0
		 */
		default Map<String, String> asMap() {

			return Map.of("version", this.getVersion(), "description", this.getOptionalDescription().orElse(""), "type",
					this.getType().name(), "installedOn", this.getInstalledOn().map(ZonedDateTime::toString).orElse(""),
					"installedBy", this.getInstalledBy().orElse(""), "executionTime",
					this.getExecutionTime().map(Duration::toString).orElse(""), "state", this.getState().name(),
					"source", this.getSource());
		}

	}

}
