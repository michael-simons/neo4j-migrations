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
package ac.simons.neo4j.migrations.core.refactorings;

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 * @author Michael J. Simons
 * @soundtrack Soilwork - Natural Born Chaos
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

	private DefaultNormalize(String property, List<Object> trueValues, List<Object> falseValues, String customQuery, Integer batchSize) {
		super(customQuery, batchSize);


		boolean nullIsTrue = trueValues.stream().anyMatch(DefaultNormalize::isNull);
		boolean nullIsFalse = falseValues.stream().anyMatch(DefaultNormalize::isNull);

		if (nullIsFalse && nullIsTrue) {
			throw new IllegalArgumentException("Both true and false values contain the literal value null");
		}

		trueValues.stream().filter(falseValues::contains)
			.findFirst().ifPresent(v -> {
				throw new IllegalArgumentException("Both true and false values contain `" + v + "`");
			});

		this.property = property;
		this.trueValues = trueValues;
		this.falseValues = falseValues;

		if (this.batchSize != null) {
			this.featureSet = QueryRunner.defaultFeatureSet()
				.withRequiredVersion("4.4")
				.withBatchingSupport(true);
		} else {
			this.featureSet = QueryRunner.defaultFeatureSet()
				.withRequiredVersion("4.1");
		}
	}

	static boolean isNull(Object v) {
		return v == null || Values.NULL.equals(v);
	}

	QueryRunner.FeatureSet getFeatures() {
		return featureSet;
	}

	@Override
	public Normalize inBatchesOf(Integer newBatchSize) {
		if (newBatchSize != null && newBatchSize < 1) {
			throw new IllegalArgumentException("Batch size must be either null or equal or greater one");
		}

		return Objects.equals(this.batchSize, newBatchSize) ?
			this :
			new DefaultNormalize(this.property, this.trueValues, this.falseValues, this.customQuery, newBatchSize);
	}

	@Override
	public Normalize withCustomQuery(String newCustomQuery) {
		String value = filterCustomQuery(newCustomQuery);

		return Objects.equals(this.customQuery, value) ?
			this :
			new DefaultNormalize(this.property, this.trueValues, this.falseValues, value, this.batchSize);
	}

	@Override
	public Counters apply(RefactoringContext refactoringContext) {
		try (QueryRunner queryRunner = refactoringContext.getQueryRunner(featureSet)) {
			return new DefaultCounters(queryRunner.run(generateQuery(refactoringContext::findSingleResultIdentifier)).consume().counters());
		}
	}

	Query generateQuery(Function<String, Optional<String>> elementExtractor) {

		List<Object> tv = trueValues;
		List<Object> fv = falseValues;
		Boolean nullValue = null;
		Predicate<Object> isNull = DefaultNormalize::isNull;
		if (trueValues.stream().anyMatch(isNull)) {
			nullValue = true;
			tv = trueValues.stream().filter(isNull.negate()).collect(Collectors.toList());
		} else if (falseValues.stream().anyMatch(DefaultNormalize::isNull)) {
			nullValue = false;
			fv = falseValues.stream().filter(isNull.negate()).collect(Collectors.toList());
		}

		String varName;
		String innerQuery;
		if (customQuery == null) {
			varName = "t";
			innerQuery = "MATCH (n) RETURN n AS t UNION ALL MATCH ()-[r]->() RETURN r AS t";
		} else {
			varName = elementExtractor.apply(customQuery).orElseThrow(IllegalArgumentException::new);
			innerQuery = customQuery;
		}

		String quotedProperty = Strings.escapeIfNecessary(this.property);
		String formatString = ""
			+ "CALL { %2$s } WITH %3$s AS e\n"
			+ "<FILTER />\n"
			+ "<BATCH>SET e.%1$s = CASE\n"
			+ "  WHEN e.%1$s IN $trueValues THEN true\n"
			+ "  WHEN e.%1$s IN $falseValues THEN false\n"
			+ "  ELSE $nullValue\n"
			+ "END</BATCH>";

		if (batchSize == null) {
			formatString = formatString.replaceAll("<BATCH>|</BATCH>", "");
		} else {
			formatString = formatString
				.replace("<BATCH>", "CALL { WITH e ")
				.replace("</BATCH>", " } IN TRANSACTIONS OF %4$d ROWS");
		}
		// If the property does not exist and the value for non-existing properties is undefined, we can reduce the
		// number of touched entities.
		formatString = formatString
			.replace("<FILTER />\n", nullValue == null ? "WHERE e.%1$s IS NOT NULL\n" : "");

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("trueValues", Values.value(tv));
		parameters.put("falseValues", Values.value(fv));
		parameters.put("nullValue", Values.value(nullValue));
		return new Query(String.format(formatString, quotedProperty, innerQuery, varName, batchSize), parameters);
	}
}
