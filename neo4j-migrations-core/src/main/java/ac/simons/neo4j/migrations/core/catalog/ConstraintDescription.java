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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.Neo4jEdition;

/**
 * Contextual information passed to parsers. It is an enriched version of a structured description coming from a given
 * datbaase method.
 *
 * @author michael J. Simons
 * @since TBA
 */
final class ConstraintDescription extends AbstractContext {

	/**
	 * An optional name, might be {@literal null} in case the neo4j version used didn't have names yet.
	 */
	private final String name;

	/**
	 * The description as returned by the database.
	 */
	private final String value;

	ConstraintDescription(String version, Neo4jEdition edition, String name, String value) {
		super(version, edition);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}
