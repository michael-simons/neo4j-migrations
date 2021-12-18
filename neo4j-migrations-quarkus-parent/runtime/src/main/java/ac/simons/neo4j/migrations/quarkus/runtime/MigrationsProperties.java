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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.List;
import java.util.Optional;

/**
 * Shim between {@link ac.simons.neo4j.migrations.core.MigrationsConfig} and the Smallrye configuration.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@ConfigRoot(name = "neo4j.migrations", prefix = "org", phase = ConfigPhase.RUN_TIME)
public class MigrationsProperties {

	/**
	 * Set to {@literal false} to disable migrations at start. An instance of {@link ac.simons.neo4j.migrations.core.Migrations} will
	 * still be provided to interested parties.
	 */
	@ConfigItem(defaultValue = "true")
	public boolean enabled;

	/**
	 * List of packages to scan for Java migrations.
	 */
	@ConfigItem
	public Optional<List<String>> packagesToScan;

	/**
	 * Locations of migrations scripts.
	 */
	@ConfigItem(defaultValue = Defaults.LOCATIONS_TO_SCAN_VALUE)
	public List<String> locationsToScan;

	/**
	 * The transaction mode in use (Defaults to "per migration", meaning one script is run in one transaction).
	 */
	@ConfigItem(defaultValue = Defaults.TRANSACTION_MODE_VALUE)
	public MigrationsConfig.TransactionMode transactionMode;

	/**
	 * The database that should be migrated (Neo4j EE 4.0+ only). Leave empty for using the default database.
	 */
	@ConfigItem
	public Optional<String> database;

	/**
	 * The database that should be used for storing informations about migrations (Neo4j EE 4.0+ only). Leave empty for using the default database.
	 */
	@ConfigItem
	public Optional<String> schemaDatabase;

	/**
	 * An alternative user to impersonate during migration. Might have higher privileges than the user connected, which
	 * will be dropped again after migration. Requires Neo4j EE 4.4+. Leave empty for using the connected user.
	 */
	@ConfigItem
	public Optional<String> impersonatedUser;

	/**
	 * Username recorded as property {@literal by} on the MIGRATED_TO relationship.
	 */
	@ConfigItem
	public Optional<String> installedBy;

	/**
	 * Validating helps you verify that the migrations applied to the database match the ones available locally and is on by default.
	 */
	@ConfigItem(defaultValue = Defaults.VALIDATE_ON_MIGRATE_VALUE)
	public boolean validateOnMigrate;

	/**
	 * If you’re programming on Windows and working with people who are not (or vice-versa), you’ll probably run into
	 * line-ending issues at some point. This is because Windows uses both a carriage-return character and a linefeed
	 * character for newlines in its files, whereas macOS and Linux systems use only the linefeed character.
	 * This is a subtle but incredibly annoying fact of cross-platform work; many editors on Windows silently replace
	 * existing LF-style line endings with CRLF, or insert both line-ending characters when the user hits the enter key.
	 * Neo4j migrations can handle this by auto-converting CRLF line endings into LF before computing checksums of a
	 * Cypher-based migration or applying it.
	 */
	@ConfigItem(defaultValue = Defaults.AUTOCRLF_VALUE)
	public boolean autocrlf;
}
