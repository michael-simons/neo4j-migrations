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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Public information about an applied migration. All migrations (applied and pending) form a chain of transformations
 * to a database. The chain starts implicit with a baseline version. The baseline version is not contained in this chain.
 *
 * @author Michael J. Simons
 * @soundtrack Paul van Dyk - From Then On
 * @since 0.0.4
 */
public sealed interface MigrationChain extends ConnectionDetails permits DefaultMigrationChain {

	/**
	 * Creates an empty instance of a {@link MigrationChain}. Can be useful for testing.
	 *
	 * @return An empty migration chain
	 * @since 2.3.0
	 */
	static MigrationChain empty() {
		return new DefaultMigrationChain(ConnectionDetails.of(null, null, null, null, null, null), Map.of());
	}

	/**
	 * Used for selecting how the {@link MigrationChain} should be computed.
	 *
	 * @see Migrations#info(ChainBuilderMode)
	 * @since 1.4.0
	 */
	enum ChainBuilderMode {

		/**
		 * Create the chain of applied and pending migrations by comparing the locally discovered migrations
		 * and the migrations applied remotely. Validation will be performed (such as checking for the correct number of
		 * migrations and checksum comparison).
		 */
		COMPARE,
		/**
		 * Build a chain only based on locally discovered migrations and assume all migrations are pending. No validation
		 * will be performed.
		 */
		LOCAL,
		/**
		 * Build a chain only based on remotely applied migrations and assume all migrations are applied. No validation
		 * will be performed.
		 */
		REMOTE
	}

	/**
	 * Pretty prints this chain as an ASCII table.
	 *
	 * @return A formatted string (an ASCII table representing the chain)
	 * @since 0.0.11
	 */
	default String prettyPrint() {

		StringBuilder sb = new StringBuilder(ConnectionDetailsFormatter.INSTANCE.format(this))
			.append(MigrationChainFormat.LS)
			.append(MigrationChainFormat.LS);

		if (getElements().isEmpty()) {
			sb.append(MigrationChainFormat.LS).append("No migrations found.");
		} else {
			MigrationChainFormat.formatElements(this, sb);
		}
		return sb.toString();
	}

	/**
	 * @param version An arbitrary version string
	 * @return True, if the version string maps to a migration that has been applied.
	 */
	boolean isApplied(String version);

	/**
	 * @return The elements of this migration
	 */
	Collection<Element> getElements();

	/**
	 * @return The length of this chain.
	 * @since 2.2.0
	 */
	default int length() {
		return getElements().size();
	}

	/**
	 * @return the last applied version
	 * @since 1.11.0
	 */
	default Optional<MigrationVersion> getLastAppliedVersion() {
		return Optional.empty();
	}

	/**
	 * A chain element describing a pending or applied migration.
	 */
	sealed interface Element permits DefaultMigrationChainElement {

		/**
		 * @return State of this migration.
		 */
		MigrationState getState();

		/**
		 * @return The type of the migration.
		 */
		MigrationType getType();

		/**
		 * @return The checksum of this migration if available.
		 */
		Optional<String> getChecksum();

		/**
		 * @return The schema version after the migration is complete.
		 */
		String getVersion();

		/**
		 * @return An optional description of the migration represented by this element.
		 * @since 1.9.0
		 */
		Optional<String> getOptionalDescription();

		/**
		 * @return The name of the script or class on which this migration is based.
		 */
		String getSource();

		/**
		 * @return The timestamp when this migration was installed. (Only for applied migrations)
		 */
		Optional<ZonedDateTime> getInstalledOn();

		/**
		 * @return The user that installed this migration. (Only for applied migrations)
		 */
		Optional<String> getInstalledBy();

		/**
		 * @return The execution time of this migration. (Only for applied migrations)
		 */
		Optional<Duration> getExecutionTime();
	}
}
