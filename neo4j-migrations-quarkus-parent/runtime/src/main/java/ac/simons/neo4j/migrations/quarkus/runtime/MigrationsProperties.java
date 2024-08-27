/*
 * Copyright 2020-2024 the original author or authors.
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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.VersionSortOrder;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Shim between {@link ac.simons.neo4j.migrations.core.MigrationsConfig} and the Smallrye configuration.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@ConfigMapping(prefix = "org.neo4j.migrations")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MigrationsProperties {

	/**
	 * An optional list of external locations that don't become part of the image. Those locations can be changed during
	 * runtime in contrast to {@link MigrationsBuildTimeProperties#locationsToScan} but can only be used for locations
	 * inside the filesystem.
	 */
	Optional<List<String>> externalLocations();

	/**
	 * Set to {@literal false} to disable migrations at start. An instance of {@link ac.simons.neo4j.migrations.core.Migrations} will
	 * still be provided to interested parties.
	 */
	@WithDefault("true")
	boolean enabled();

	/**
	 * The transaction mode in use (Defaults to "per migration", meaning one script is run in one transaction).
	 */
	@WithDefault(Defaults.TRANSACTION_MODE_VALUE)
	MigrationsConfig.TransactionMode transactionMode();

	/**
	 * The database that should be migrated (Neo4j EE 4.0+ only). Leave empty for using the default database.
	 */
	Optional<String> database();

	/**
	 * The database that should be used for storing informations about migrations (Neo4j EE 4.0+ only). Leave empty for using the default database.
	 */
	Optional<String> schemaDatabase();

	/**
	 * An alternative user to impersonate during migration. Might have higher privileges than the user connected, which
	 * will be dropped again after migration. Requires Neo4j EE 4.4+. Leave empty for using the connected user.
	 */
	Optional<String> impersonatedUser();

	/**
	 * Username recorded as property {@literal by} on the MIGRATED_TO relationship.
	 */
	Optional<String> installedBy();

	/**
	 * Validating helps you verify that the migrations applied to the database match the ones available locally and is on by default.
	 */
	@WithDefault(Defaults.VALIDATE_ON_MIGRATE_VALUE)
	boolean validateOnMigrate();

	/**
	 * If you're programming on Windows and working with people who are not (or vice-versa), you'll probably run into
	 * line-ending issues at some point. This is because Windows uses both a carriage-return character and a linefeed
	 * character for newlines in its files, whereas macOS and Linux systems use only the linefeed character.
	 * This is a subtle but incredibly annoying fact of cross-platform work; many editors on Windows silently replace
	 * existing LF-style line endings with CRLF, or insert both line-ending characters when the user hits the enter key.
	 * Neo4j migrations can handle this by auto-converting CRLF line endings into LF before computing checksums of a
	 * Cypher-based migration or applying it.
	 */
	@WithDefault(Defaults.AUTOCRLF_VALUE)
	boolean autocrlf();

	/**
	 * A configurable delay that will be applied in between applying two migrations.
	 */
	Optional<Duration> delayBetweenMigrations();

	/**
	 * The sort order for migrations. Defaults to {@link VersionSortOrder#LEXICOGRAPHIC} until 3.x.
	 */
	@WithDefault(Defaults.VERSION_SORT_ORDER_VALUE)
	VersionSortOrder versionSortOrder();
}
