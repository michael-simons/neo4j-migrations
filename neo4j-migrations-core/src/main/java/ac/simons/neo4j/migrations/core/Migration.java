/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.Optional;

/**
 * Base interface for any migration.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public interface Migration {

	/**
	 * @return The version.
	 */
	MigrationVersion getVersion();

	/**
	 * @return Some description.
	 * @deprecated Since 1.9.0 see {@link #getOptionalDescription()}
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	String getDescription();

	/**
	 * @return An optional description of this migration.
	 */
	@SuppressWarnings("squid:S1874")
	default Optional<String> getOptionalDescription() {
		return Optional.ofNullable(getDescription());
	}

	/**
	 * @return Something that describes the source of this migration.
	 */
	String getSource();

	/**
	 * @return Possible checksum of the migration.
	 */
	default Optional<String> getChecksum() {
		return Optional.empty();
	}

	/**
	 * Implement your migration code here.
	 *
	 * @param context The migrations' context.
	 * @throws MigrationsException In case anything happens, wrap your exception or create a new one
	 */
	void apply(MigrationContext context);
}
