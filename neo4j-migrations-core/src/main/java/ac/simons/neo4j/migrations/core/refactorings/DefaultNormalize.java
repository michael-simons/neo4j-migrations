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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 * Implements the {@link Normalize normalize-refactoring}.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
final class DefaultNormalize extends AbstractCustomizableRefactoring implements Normalize {

	private final String property;

	private final List<Object> trueValues;

	private final List<Object> falseValues;

	private final QueryRunner.FeatureSet featureSet;

	DefaultNormalize(String property, List<Object> trueValues, List<Object> falseValues) {
		this(property, trueValues, falseValues, null, null);
	}

	private DefaultNormalize(String property, List<Object> trueValues, List<Object> falseValues, String customQuery,
			Integer batchSize) {
		super(customQuery, batchSize);

		boolean nullIsTrue = trueValues.stream().anyMatch(DefaultNormalize::isNull);
		boolean nullIsFalse = falseValues.stream().anyMatch(DefaultNormalize::isNull);

		if (nullIsFalse && nullIsTrue) {
			throw new IllegalArgumentException("Both true and false values contain the literal value null");
		}

		trueValues.stream().filter(falseValues::contains).findFirst().ifPresent(v -> {
			throw new IllegalArgumentException("Both true and false values contain `" + v + "`");
		});

		this.property = property;
		this.trueValues = trueValues;
		this.falseValues = falseValues;

		if (this.batchSize != null) {
			this.featureSet = QueryRunner.defaultFeatureSet().withRequiredVersion("4.4").withBatchingSupport(true);
		}
		else {
			this.featureSet = QueryRunner.defaultFeatureSet().withRequiredVersion("4.1");
		}
	}

	static boolean isNull(Object v) {
		return v == null || Values.NULL.equals(v);
	}

	QueryRunner.FeatureSet getFeatures() {
		return this.featureSet;
	}

	@Override
	public Normalize inBatchesOf(Integer newBatchSize) {

		return inBatchesOf0(newBatchSize, DefaultNormalize.class,
				v -> new DefaultNormalize(this.property, this.trueValues, this.falseValues, this.customQuery, v));
	}

	@Override
	public Normalize withCustomQuery(String newCustomQuery) {

		return withCustomQuery0(newCustomQuery, Normalize.class,
				v -> new DefaultNormalize(this.property, this.trueValues, this.falseValues, v, this.batchSize));
	}

	@Override
	public Counters apply(RefactoringContext context) {
		try (QueryRunner queryRunner = context.getQueryRunner(this.featureSet)) {
			return new DefaultCounters(
					queryRunner.run(generateQuery(context::sanitizeSchemaName, context::findSingleResultIdentifier))
						.consume()
						.counters());
		}
	}

	Query generateQuery(UnaryOperator<String> sanitizer, Function<String, Optional<String>> elementExtractor) {

		List<Object> tv = this.trueValues;
		List<Object> fv = this.falseValues;
		Boolean nullValue = null;
		Predicate<Object> isNull = DefaultNormalize::isNull;
		if (this.trueValues.stream().anyMatch(isNull)) {
			nullValue = true;
			tv = this.trueValues.stream().filter(isNull.negate()).toList();
		}
		else if (this.falseValues.stream().anyMatch(DefaultNormalize::isNull)) {
			nullValue = false;
			fv = this.falseValues.stream().filter(isNull.negate()).toList();
		}

		String varName;
		String innerQuery;
		if (this.customQuery == null) {
			varName = "t";
			innerQuery = "MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t";
		}
		else {
			varName = elementExtractor.apply(this.customQuery).orElseThrow(IllegalArgumentException::new);
			innerQuery = this.customQuery;
		}

		String quotedProperty = sanitizer.apply(this.property);
		String formatString = """
				CALL { %2$s } WITH %3$s AS e
				<FILTER />
				<BATCH>SET e.%1$s = CASE
				  WHEN e.%1$s IN $trueValues THEN true
				  WHEN e.%1$s IN $falseValues THEN false
				  WHEN e.%1$s IN [true, false] THEN e.%1$s
				  ELSE $nullValue
				END</BATCH>""";

		if (this.batchSize == null) {
			formatString = formatString.replaceAll("<BATCH>|</BATCH>", "");
		}
		else {
			formatString = formatString.replace("<BATCH>", "CALL { WITH e ")
				.replace("</BATCH>", " } IN TRANSACTIONS OF %4$d ROWS");
		}
		// If the property does not exist and the value for non-existing properties is
		// undefined, we can reduce the number of touched entities.
		formatString = formatString.replace("<FILTER />\n", (nullValue != null) ? "" : "WHERE e.%1$s IS NOT NULL\n");

		Map<String, Object> parameters = Map.of("trueValues", Values.value(tv), "falseValues", Values.value(fv),
				"nullValue", Values.value(nullValue));
		return new Query(String.format(formatString, quotedProperty, innerQuery, varName, this.batchSize), parameters);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultNormalize that = (DefaultNormalize) o;
		return this.property.equals(that.property) && this.trueValues.equals(that.trueValues)
				&& this.falseValues.equals(that.falseValues) && Objects.equals(this.customQuery, that.customQuery)
				&& Objects.equals(this.batchSize, that.batchSize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.property, this.trueValues, this.falseValues, this.customQuery, this.batchSize);
	}

}
