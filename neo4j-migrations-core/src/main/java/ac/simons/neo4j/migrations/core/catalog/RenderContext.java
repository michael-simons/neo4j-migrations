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

import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Contextual information passed to renderers.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
final class RenderContext {

	private static final Set<String> PRIOR_TO_44 = Stream.concat(Stream.of("3.5"),
		IntStream.range(0, 4).mapToObj(i -> "4." + i)).collect(
		Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));

	static RenderContext defaultContext() {
		return new RenderContext(null, Neo4jEdition.UNDEFINED, null, true);
	}

	/**
	 * Neo4j version used to get any of the contained information.
	 */
	private final Neo4jVersion version;

	/**
	 * Neo4j edition used to get any of the contained information.
	 */
	private final Neo4jEdition edition;

	private final Operator operator;

	private final boolean idempotent;

	RenderContext(String version, Neo4jEdition edition, Operator operator, boolean idempotent) {
		this.version = Neo4jVersion.of(version);
		this.edition = edition;
		this.operator = operator;
		this.idempotent = idempotent;
	}


	public Neo4jVersion getVersion() {
		return version;
	}

	public Neo4jEdition getEdition() {
		return edition;
	}

	public Operator getOperator() {
		return operator;
	}

	public boolean isIdempotent() {
		return idempotent;
	}

	public boolean isVersionPriorTo44() {
		return getVersion().isPriorTo44();
	}
}
