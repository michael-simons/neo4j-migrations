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

import java.util.Locale;

/**
 * Represents a Neo4j edition.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public enum Neo4jEdition {

	/**
	 * Constant for the enterprise edition.
	 */
	ENTERPRISE,
	/**
	 * Constant for the community edition.
	 */
	COMMUNITY,
	/**
	 * Constant for an unknown edition.
	 */
	UNDEFINED;

	/**
	 * Creates a new Neo4j edition by parsing the edition string in a lenient fashion.
	 * @param edition a string value describing the edition, may be {@literal null}
	 * @return the enum constant for the given edition or {@link Neo4jEdition#UNDEFINED}
	 * if lenient parsing is not possible.
	 */
	public static Neo4jEdition of(String edition) {
		return of(edition, true);
	}

	/**
	 * Creates a new Neo4j edition by parsing the edition string, optionally in a lenient
	 * fashion.
	 * @param edition a string value describing the edition, may be {@literal null}
	 * @param lenient whether lenient parsing should be attempted or not
	 * @return the enum constant for the given edition or {@link Neo4jEdition#UNDEFINED}
	 * if lenient parsing is not possible.
	 */
	public static Neo4jEdition of(String edition, boolean lenient) {

		if (edition == null) {
			if (!lenient) {
				throw new IllegalArgumentException("Cannot derive an edition from literal null");
			}
			return Neo4jEdition.UNDEFINED;
		}
		else {
			String value = edition.trim().toUpperCase(Locale.ROOT);
			if (!lenient) {
				return Neo4jEdition.valueOf(value);
			}
			return switch (value) {
				case "ENTERPRISE" -> Neo4jEdition.ENTERPRISE;
				case "COMMUNITY" -> Neo4jEdition.COMMUNITY;
				default -> Neo4jEdition.UNDEFINED;
			};
		}
	}

}
