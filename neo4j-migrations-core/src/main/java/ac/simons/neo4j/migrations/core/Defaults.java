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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import ac.simons.neo4j.migrations.core.MigrationsConfig.CypherVersion;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.MigrationsConfig.VersionSortOrder;

/**
 * Defaults for Migrations configuration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
public final class Defaults {

	/**
	 * The default Neo4j user to use.
	 */
	public static final String DEFAULT_USER = "neo4j";

	/**
	 * Default script extension to look for.
	 */
	public static final String CYPHER_SCRIPT_EXTENSION = "cypher";

	/**
	 * Default encoding for Cypher scripts.
	 */
	public static final Charset CYPHER_SCRIPT_ENCODING = StandardCharsets.UTF_8;

	/**
	 * Default package or folder name for locations to scan, agnostic to classpath or
	 * filesystem.
	 */
	public static final String LOCATIONS_TO_SCAN_WITHOUT_PREFIX = "neo4j/migrations";

	/**
	 * Default locations to scan (with a {@link Location.LocationType} prefix).
	 */
	public static final String LOCATIONS_TO_SCAN_VALUE = "classpath:" + LOCATIONS_TO_SCAN_WITHOUT_PREFIX;

	/**
	 * Default locations to scan (with a {@link Location.LocationType} prefix).
	 */
	public static final List<String> LOCATIONS_TO_SCAN = Collections.singletonList(LOCATIONS_TO_SCAN_VALUE);

	/**
	 * Same as {@link #LOCATIONS_TO_SCAN} but as {@link String string value} to be used in
	 * configuration that requires defaults given as string.
	 */
	public static final String TRANSACTION_MODE_VALUE = "PER_MIGRATION";

	/**
	 * Default transaction mode to use.
	 */
	public static final TransactionMode TRANSACTION_MODE = TransactionMode.PER_MIGRATION;

	/**
	 * Default setting for {@code validateOnMigrate}.
	 */
	public static final boolean VALIDATE_ON_MIGRATE = true;

	/**
	 * Same as {@link #VALIDATE_ON_MIGRATE} but as {@link String string value} to be used
	 * in configuration that requires defaults given as string.
	 */
	public static final String VALIDATE_ON_MIGRATE_VALUE = "true";

	/**
	 * Default setting for {@code autocrlf}.
	 */
	public static final boolean AUTOCRLF = false;

	/**
	 * Same as {@link #AUTOCRLF} but as {@link String string value} to be used in
	 * configuration that requires defaults given as string.
	 */
	public static final String AUTOCRLF_VALUE = "false";

	/**
	 * Same as {@link #VERSION_SORT_ORDER} but as {@link String string value} to be used
	 * in configuration that requires defaults given as string.
	 */
	public static final String VERSION_SORT_ORDER_VALUE = "SEMANTIC";

	/**
	 * The sort order used for version numbers, please see {@link VersionSortOrder}.
	 * @since 2.9.0
	 */
	public static final VersionSortOrder VERSION_SORT_ORDER = VersionSortOrder.SEMANTIC;

	/**
	 * Default setting for {@code outOfOrder}.
	 * @since 2.14.0
	 */
	public static final boolean OUT_OF_ORDER = false;

	/**
	 * Same as {@link #OUT_OF_ORDER} but as a {@link String string value} to be used in
	 * configuration that requires defaults given as string.
	 */
	public static final String OUT_OF_ORDER_VALUE = "false";

	/**
	 * Default setting for {@code useFlywayCompatibleChecksums}.
	 * @since 2.17.0
	 */
	public static final boolean USE_FLYWAY_COMPATIBLE_CHECKSUMS = false;

	/**
	 * Default setting for {@code useFlywayCompatibleChecksums} but as a {@link String
	 * string value} to be used in configuration that requires defaults given as string.
	 * @since 2.17.0
	 */
	public static final String USE_FLYWAY_COMPATIBLE_CHECKSUMS_VALUE = "false";

	/**
	 * Default setting for {@code cypherVersion}.
	 * @since 2.19.0
	 */
	public static final CypherVersion CYPHER_VERSION = CypherVersion.DATABASE_DEFAULT;

	/**
	 * Default setting for {@code cypherVersion} but as a {@link String string value} to
	 * be used in configuration that requires defaults given as string.
	 * @since 2.19.0
	 */
	public static final String CYPHER_VERSION_VALUE = "DATABASE_DEFAULT";

	/**
	 * Default packages to scan.
	 */
	static final List<String> PACKAGES_TO_SCAN = Collections.emptyList();

	private Defaults() {
	}

}
