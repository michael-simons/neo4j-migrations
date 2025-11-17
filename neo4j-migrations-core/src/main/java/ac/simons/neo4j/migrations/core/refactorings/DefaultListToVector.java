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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;

final class DefaultListToVector extends AbstractCustomizableRefactoring implements ListToVector {

	private final Target target;

	private final Set<String> identifiers;

	private final String property;

	private final ElementType elementType;

	private final QueryRunner.FeatureSet featureSet;

	DefaultListToVector(Target target, Collection<String> identifiers, String property, String customQuery,
			Integer batchSize, ElementType elementType) {
		super(customQuery, batchSize);
		this.target = Objects.requireNonNull(target);
		this.identifiers = (identifiers != null)
				? identifiers.stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new))
				: Collections.emptySet();
		this.property = Objects.requireNonNullElse(property, DEFAULT_PROPERTY_NAME);
		this.featureSet = QueryRunner.defaultFeatureSet().withRequiredVersion("2025.10").withBatchingSupport(true);
		this.elementType = Objects.requireNonNullElse(elementType, ElementType.FLOAT);
	}

	@Override
	public Counters apply(RefactoringContext context) {
		try (QueryRunner queryRunner = context.getQueryRunner(this.featureSet)) {
			var query = generateQuery(context::findSingleResultIdentifier);
			return new DefaultCounters(queryRunner.run(query).consume().counters());
		}
	}

	Query generateQuery(Function<String, Optional<String>> elementExtractor) {
		String entityName = (this.customQuery == null) ? ""
				: elementExtractor.apply(this.customQuery).orElseThrow(IllegalArgumentException::new);

		var parameters = new HashMap<String, Object>();
		if (!this.identifiers.isEmpty()) {
			parameters.put("identifiers", this.identifiers);
		}
		parameters.put("property", this.property);
		if (this.batchSize != null) {
			parameters.put("batchSize", this.batchSize);
		}

		return new Query(String.format(this.target.generateFormatString(this.customQuery, this.batchSize),
				this.customQuery, entityName, this.elementType.name()), parameters);
	}

	@Override
	public ListToVector withProperty(String name) {

		if (Objects.requireNonNull(name, "Property name is required").isBlank()) {
			throw new IllegalArgumentException("Property name must not be blank");
		}
		return new DefaultListToVector(this.target, this.identifiers, name, this.customQuery, this.batchSize,
				this.elementType);
	}

	@Override
	public ListToVector withElementType(ElementType type) {
		return new DefaultListToVector(this.target, this.identifiers, this.property, this.customQuery, this.batchSize,
				type);
	}

	@Override
	public ListToVector inBatchesOf(Integer newBatchSize) {

		return inBatchesOf0(newBatchSize, ListToVector.class, v -> new DefaultListToVector(this.target,
				this.identifiers, this.property, this.customQuery, v, this.elementType));
	}

	@Override
	public ListToVector withCustomQuery(String newCustomQuery) {

		return withCustomQuery0(newCustomQuery, ListToVector.class, v -> new DefaultListToVector(this.target,
				(v != null) ? null : this.identifiers, this.property, v, this.batchSize, this.elementType));
	}

	/**
	 * Target for the property changes.
	 */
	enum Target implements FormatStringGenerator {

		/**
		 * Targets relationships.
		 */
		RELATIONSHIP(
				"CYPHER 25 MATCH ()-[r:$all($identifiers)]->() WHERE r[$property] :: LIST<FLOAT NOT NULL> NOT NULL",
				"CYPHER 25 CALL { %1$s } WITH %2$s AS r",
				"SET r[$property] = VECTOR(r[$property], size(r[$property]), %3$s)", "r"),

		/**
		 * Targets nodes.
		 */
		NODE("CYPHER 25 MATCH (n:$all($identifiers)) WHERE n[$property] :: LIST<FLOAT NOT NULL> NOT NULL",
				"CYPHER 25 CALL { %1$s } WITH %2$s AS n",
				"SET n[$property] = VECTOR(n[$property], size(n[$property]), %3$s)", "n");

		private final Fragments fragments;

		Target(String sourceFragment, String sourceFragmentWithCustomQuery, String actionFragment, String target) {

			this.fragments = new Fragments(sourceFragment, sourceFragmentWithCustomQuery, actionFragment,
					"CALL(%s) { %s } IN TRANSACTIONS OF $batchSize ROWS".formatted(target, actionFragment));
		}

		@Override
		public Fragments getFragments() {
			return this.fragments;
		}

	}

}
