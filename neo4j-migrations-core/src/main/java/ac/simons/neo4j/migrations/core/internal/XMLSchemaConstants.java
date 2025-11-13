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
package ac.simons.neo4j.migrations.core.internal;

import java.util.Set;

/**
 * Constants used throughout parsing and reading / writing elements and attributes of xml
 * and database constraints.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class XMLSchemaConstants {

	/**
	 * The namespace constant.
	 */
	public static final String MIGRATION_NS = "https://michael-simons.github.io/neo4j-migrations";

	/**
	 * Constant for {@literal migration}.
	 */
	public static final String MIGRATION = "migration";

	/**
	 * Constant for {@literal type} (both used as attribute and element).
	 */
	public static final String TYPE = "type";

	/**
	 * Constant for the element {@literal properties}.
	 */
	public static final String PROPERTIES = "properties";

	/**
	 * Constant for the attribute {@literal name}.
	 */
	public static final String NAME = "name";

	/**
	 * Constant for the element {@literal constraint}.
	 */
	public static final String CONSTRAINT = "constraint";

	/**
	 * Constant for the element {@literal label}.
	 */
	public static final String LABEL = "label";

	/**
	 * Constant for the element {@literal property}.
	 */
	public static final String PROPERTY = "property";

	/**
	 * Constant for the element {@literal options}.
	 */
	public static final String OPTIONS = "options";

	/**
	 * Constant for the element {@literal catalog}.
	 */
	public static final String CATALOG = "catalog";

	/**
	 * Constant for the element {@literal indexes}.
	 */
	public static final String INDEX = "index";

	/**
	 * Constant for the element {@literal indexes}.
	 */
	public static final String INDEXES = "indexes";

	/**
	 * Constant for the element {@literal constraints}.
	 */
	public static final String CONSTRAINTS = "constraints";

	/**
	 * Constant for the element {@literal apply}.
	 */
	public static final String APPLY = "apply";

	/**
	 * Constant for the element {@literal refactor}.
	 */
	public static final String REFACTOR = "refactor";

	/**
	 * Constant for the {@literal reset} attribute.
	 */
	public static final String RESET = "reset";

	/**
	 * Constant for the element names of supported operations.
	 */
	public static final Set<String> SUPPORTED_OPERATIONS = Set.of("verify", "create", "drop", APPLY, REFACTOR);

	private XMLSchemaConstants() {
	}

}
