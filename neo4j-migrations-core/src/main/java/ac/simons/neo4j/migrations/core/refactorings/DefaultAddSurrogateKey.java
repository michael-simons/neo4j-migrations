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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;

/**
 * Default implementation for adding surrogates.
 *
 * @author Michael J. Simons
 * @soundtrack Max Herre - MTV Unplugged Kahedi Radio Show
 * @since 1.15.2
 */
final class DefaultAddSurrogateKey extends AbstractCustomizableRefactoring implements AddSurrogateKey {

	static final Pattern OPENING_CLOSING_BRACES = Pattern.compile("\\(?.+\\)");

	/**
	 * Target of the new surrogate key
	 */
	enum Target implements FormatStringGenerator {

		/**
		 * Targets relationships.
		 */
		RELATIONSHIP(
			"MATCH ()-[r:%1$s]->() WHERE r.%2$s IS NULL",
			"CALL { %5$s } WITH %6$s AS r",
			"SET r.%2$s = %3$s",
			"CALL { WITH r SET r.%2$s = %3$s } IN TRANSACTIONS OF %4$d ROWS"
		),

		/**
		 * Targets nodes
		 */
		NODE(
			"MATCH (n:%1$s) WHERE n.%2$s IS NULL",
			"CALL { %5$s } WITH %6$s AS n",
			"SET n.%2$s = %3$s",
			"CALL { WITH n SET n.%2$s = %3$s } IN TRANSACTIONS OF %4$d ROWS"
		);

		private final Fragments fragments;

		Target(String sourceFragment, String sourceFragmentWithCustomQuery, String actionFragment, String actionFragmentWithBatchSize) {

			this.fragments = new Fragments(sourceFragment, sourceFragmentWithCustomQuery, actionFragment, actionFragmentWithBatchSize);
		}

		@Override
		public Fragments getFragments() {
			return fragments;
		}
	}

	private final Target target;

	private final Set<String> identifiers;

	private final String property;

	private final String generator;

	private final QueryRunner.FeatureSet featureSet;

	DefaultAddSurrogateKey(Target target, Collection<String> identifiers, String property, String generator) {
		this(target, identifiers, property, generator, null, null);
	}

	DefaultAddSurrogateKey(Target target, String customQuery, String property, String generator) {
		this(target, null, property, generator, customQuery, null);
	}

	private DefaultAddSurrogateKey(Target target, Collection<String> identifiers, String property, String generator, String customQuery, Integer batchSize) {
		super(customQuery, batchSize);

		this.target = target;
		this.identifiers = identifiers == null ?
			Collections.emptySet() :
			identifiers.stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
		this.property = property;
		this.generator = generator;

		if (this.batchSize != null) {
			this.featureSet = QueryRunner.defaultFeatureSet()
				.withRequiredVersion("4.4")
				.withBatchingSupport(true);
		} else if (this.customQuery != null) {
			this.featureSet = QueryRunner.defaultFeatureSet()
				.withRequiredVersion("4.1");
		} else {
			this.featureSet = QueryRunner.defaultFeatureSet()
				.withRequiredVersion("3.5");
		}
	}


	@Override
	public AddSurrogateKey withProperty(String name) {

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Property name must not be null or blank");
		}
		return new DefaultAddSurrogateKey(target, identifiers, name, generator, customQuery, batchSize);
	}

	@Override
	public AddSurrogateKey withGeneratorFunction(String name) {

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Generator function must not be null or blank");
		}
		return new DefaultAddSurrogateKey(target, identifiers, property, name, customQuery, batchSize);
	}

	@Override
	public AddSurrogateKey inBatchesOf(Integer newBatchSize) {

		return inBatchesOf0(newBatchSize,
			AddSurrogateKey.class,
			v -> new DefaultAddSurrogateKey(this.target, this.identifiers, this.property, this.generator, this.customQuery, v));
	}

	@Override
	public AddSurrogateKey withCustomQuery(String newCustomQuery) {

		return withCustomQuery0(newCustomQuery,
			AddSurrogateKey.class,
			v -> new DefaultAddSurrogateKey(this.target, v != null ? null : this.identifiers, this.property, this.generator, v, this.batchSize));
	}

	@Override
	public Counters apply(RefactoringContext context) {
		try (QueryRunner queryRunner = context.getQueryRunner(featureSet)) {
			return new DefaultCounters(queryRunner.run(generateQuery(context::sanitizeSchemaName, context::findSingleResultIdentifier)).consume().counters());
		}
	}

	static boolean isReadyMadeFunctionCall(String value) {
		return OPENING_CLOSING_BRACES.matcher(value.trim()).find();
	}


	Query generateQuery(UnaryOperator<String> sanitizer, Function<String, Optional<String>> elementExtractor) {

		String entityName = customQuery == null ? "" :
			elementExtractor.apply(customQuery).orElseThrow(IllegalArgumentException::new);

		String varName = switch (target) {
			case NODE -> "n";
			case RELATIONSHIP -> "r";
		};

		String sanitizedIdentifiers = this.identifiers.stream().map(sanitizer).collect(Collectors.joining(":"));
		String sanitizedProperty = sanitizer.apply(property);
		String generatorCall;
		if (isReadyMadeFunctionCall(generator)) {
			generatorCall = String.format(generator, varName);
		} else {
			generatorCall = generator + "()";
		}

		return new Query(String.format(target.generateFormatString(customQuery, batchSize), sanitizedIdentifiers,
			sanitizedProperty, generatorCall, batchSize, customQuery, entityName));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultAddSurrogateKey that = (DefaultAddSurrogateKey) o;
		return target == that.target && identifiers.equals(that.identifiers) && property.equals(that.property) && generator.equals(that.generator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, identifiers, property, generator);
	}

	Target getTarget() {
		return target;
	}

	Set<String> getIdentifiers() {
		return Collections.unmodifiableSet(identifiers);
	}

	String getProperty() {
		return property;
	}

	String getGenerator() {
		return generator;
	}
}
