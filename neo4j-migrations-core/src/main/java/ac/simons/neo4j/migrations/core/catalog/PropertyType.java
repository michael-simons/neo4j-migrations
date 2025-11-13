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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Locale;

/**
 * Cypher types applicable as property types in constraints. See
 * <a href="https://neo4j.com/docs/cypher-manual/current/constraints/syntax/">Constraint
 * Syntax</a>.
 *
 * @author Michael J. Simons
 * @since 2.5.0
 */
public enum PropertyType {

	/**
	 * Constant for boolean properties.
	 */
	BOOLEAN,
	/**
	 * Constant for string properties.
	 */
	STRING,
	/**
	 * Constant for integer or long properties.
	 */
	INTEGER,
	/**
	 * Constant for float or double properties.
	 */
	FLOAT,
	/**
	 * Constant for date properties.
	 */
	DATE,
	/**
	 * Constant for local time properties.
	 */
	LOCAL_TIME,
	/**
	 * Constant for zoned time properties.
	 */
	ZONED_TIME,
	/**
	 * Constant for local datetime properties.
	 */
	LOCAL_DATETIME,
	/**
	 * Constant for zone datetime properties.
	 */
	ZONED_DATETIME,
	/**
	 * Constant for duration properties.
	 */
	DURATION,
	/**
	 * Constant for point properties.
	 */
	POINT;

	static PropertyType parse(String value) {
		return PropertyType.valueOf(value.toUpperCase(Locale.ROOT).replace(" ", "_"));
	}

	String getName() {
		return name().replace("_", " ");
	}

}
