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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Defaults for Migrations configuration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
public final class Defaults {

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
	static final String CYPHER_STATEMENT_DELIMITER = ";\r?\n";

	/**
	 * Default packages to scan.
	 */
	static final String[] PACKAGES_TO_SCAN = new String[0];

	/**
	 * Default locations to scan.
	 */
	public static final String[] LOCATIONS_TO_SCAN = new String[] { "classpath:neo4j/migrations" };

	public static final String TRANSACTION_MODE_VALUE = "PER_MIGRATION";

	/**
	 * Default transaction mode to use.
	 */
	public static final TransactionMode TRANSACTION_MODE = TransactionMode.PER_MIGRATION;

	private Defaults() {
	}
}
