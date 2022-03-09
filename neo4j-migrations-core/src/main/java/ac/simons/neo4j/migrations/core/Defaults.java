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

import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import ac.simons.neo4j.migrations.core.internal.Strings;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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
	 * Cypher delimiter
	 */
	static final String CYPHER_STATEMENT_DELIMITER = ";(:?" + Strings.LINE_DELIMITER + ")";

	/**
	 * Default packages to scan.
	 */
	static final List<String> PACKAGES_TO_SCAN = Collections.emptyList();

	/**
	 * Default package or folder name for locations to scan, agnostic to classpath or filesystem.
	 */
	public static final String LOCATIONS_TO_SCAN_WITHOUT_PREFIX = "neo4j/migrations";

	/**
	 * Default locations to scan (with a {@link ac.simons.neo4j.migrations.core.internal.Location.LocationType} prefix).
	 */
	public static final String LOCATIONS_TO_SCAN_VALUE = "classpath:" + LOCATIONS_TO_SCAN_WITHOUT_PREFIX;

	/**
	 * Default locations to scan (with a {@link ac.simons.neo4j.migrations.core.internal.Location.LocationType} prefix).
	 */
	public static final List<String> LOCATIONS_TO_SCAN = Collections.singletonList(LOCATIONS_TO_SCAN_VALUE);

	/**
	 * Same as {@link #LOCATIONS_TO_SCAN} but as {@link String string value} to be used in configuration that requires defaults given as string.
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
	 * Same as {@link #VALIDATE_ON_MIGRATE} but as {@link String string value} to be used in configuration that requires
	 * defaults given as string.
	 */
	public static final String VALIDATE_ON_MIGRATE_VALUE = "true";

	/**
	 * Default setting for {@code autocrlf}.
	 */
	public static final boolean AUTOCRLF = false;

	/**
	 * Same as {@link #AUTOCRLF} but as {@link String string value} to be used in configuration that requires defaults
	 * given as string.
	 */
	public static final String AUTOCRLF_VALUE = "false";

	private Defaults() {
	}
}
