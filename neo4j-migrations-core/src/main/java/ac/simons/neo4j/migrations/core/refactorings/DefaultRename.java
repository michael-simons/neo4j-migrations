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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.neo4j.driver.Query;

/**
 * @author Michael J. Simons
 * @soundtrack Nightwish - Decades: Live In Buenos Aires
 * @since 1.10.0
 */
final class DefaultRename extends AbstractCustomizableRefactoring implements Rename {

	/**
	 * Target of the renaming
	 */
	enum Target implements FormatStringGenerator {

		/**
		 * Targets labels.
		 */
		LABEL(
			"MATCH (s:%1$s)",
			"CALL { %4$s } WITH %5$s AS s",
			"REMOVE s:%1$s SET s:%2$s",
			"CALL { WITH s REMOVE s:%1$s SET s:%2$s } IN TRANSACTIONS OF %3$d ROWS"
		),
		/**
		 * Targets types
		 */
		TYPE(
			"MATCH (a)-[old:%1$s]->(b)",
			"CALL { %4$s } WITH %5$s AS old, startNode(%5$s) AS a, endNode(%5$s) AS b",
			"CREATE (a)-[new:%2$s]->(b) SET new+=old DELETE old",
			"CALL { WITH old, a, b CREATE (a)-[new:%2$s]->(b) SET new+=old DELETE old } IN TRANSACTIONS OF %3$d ROWS"
		),
		/**
		 * Targets node properties.
		 */
		NODE_PROPERTY(
			"MATCH (s) WHERE s.%1$s IS NOT NULL",
			"CALL { %4$s } WITH %5$s AS s WHERE s.%1$s IS NOT NULL",
			"SET s.%2$s = s.%1$s REMOVE s.%1$s",
			"CALL { WITH s SET s.%2$s = s.%1$s REMOVE s.%1$s } IN TRANSACTIONS OF %3$d ROWS"
		),
		/**
		 * Targets relationship properties
		 */
		REL_PROPERTY(
			"MATCH (a)-[r]->(b) WHERE r.%1$s IS NOT NULL",
			"CALL { %4$s } WITH %5$s AS r WHERE r.%1$s IS NOT NULL",
			"SET r.%2$s = r.%1$s REMOVE r.%1$s",
			"CALL { WITH r SET r.%2$s = r.%1$s REMOVE r.%1$s } IN TRANSACTIONS OF %3$d ROWS"
		);

		private final String sourceFragment;
		private final String sourceFragmentWithCustomQuery;
		private final String actionFragment;
		private final String actionFragmentWithBatchSize;

		Target(String sourceFragment, String sourceFragmentWithCustomQuery, String actionFragment, String actionFragmentWithBatchSize) {
			this.sourceFragment = sourceFragment;
			this.sourceFragmentWithCustomQuery = sourceFragmentWithCustomQuery;
			this.actionFragment = actionFragment;
			this.actionFragmentWithBatchSize = actionFragmentWithBatchSize;
		}

		@Override
		public String getSourceFragment() {
			return sourceFragment;
		}

		@Override
		public String getSourceFragmentWithCustomQuery() {
			return sourceFragmentWithCustomQuery;
		}

		@Override
		public String getActionFragment() {
			return actionFragment;
		}

		@Override
		public String getActionFragmentWithBatchSize() {
			return actionFragmentWithBatchSize;
		}
	}

	/**
	 * The target of this refactoring.
	 */
	private final Target target;

	/**
	 * The old value of the label, type or property to be renamed.
	 */
	private final String oldValue;

	/**
	 * The new value of the label, type or property to be renamed.
	 */
	private final String newValue;

	private final QueryRunner.FeatureSet featureSet;

	DefaultRename(Target targetEntityType, String oldValue, String newValue) {
		this(targetEntityType, oldValue, newValue, null, null);
	}

	private DefaultRename(Target target, String oldValue, String newValue, String customQuery, Integer batchSize) {
		super(customQuery, batchSize);

		this.target = target;
		this.oldValue = oldValue;
		this.newValue = newValue;

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

	QueryRunner.FeatureSet getFeatures() {
		return featureSet;
	}

	@Override
	public Counters apply(RefactoringContext context) {
		try (QueryRunner queryRunner = context.getQueryRunner(featureSet)) {
			return new DefaultCounters(queryRunner.run(generateQuery(context::sanitizeSchemaName, context::findSingleResultIdentifier)).consume().counters());
		}
	}

	@Override
	public Rename inBatchesOf(Integer newBatchSize) {
		return inBatchesOf0(newBatchSize, Rename.class, v -> new DefaultRename(this.target, this.oldValue, this.newValue, this.customQuery, v));
	}

	@Override
	public Rename withCustomQuery(String newCustomQuery) {
		return withCustomQuery0(newCustomQuery, Rename.class, v -> new DefaultRename(this.target, this.oldValue, this.newValue, v, this.batchSize));
	}

	Query generateQuery(UnaryOperator<String> sanitizer, Function<String, Optional<String>> elementExtractor) {

		String varName;
		if (customQuery == null) {
			varName = "";
		} else {
			varName = elementExtractor.apply(customQuery).orElseThrow(IllegalArgumentException::new);
		}

		return new Query(String.format(target.generateFormatString(customQuery, batchSize), sanitizer.apply(oldValue),
			sanitizer.apply(newValue), batchSize, customQuery, varName));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultRename that = (DefaultRename) o;
		return target == that.target && oldValue.equals(that.oldValue) && newValue.equals(that.newValue)
			&& Objects.equals(customQuery, that.customQuery) && Objects.equals(batchSize, that.batchSize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, oldValue, newValue, customQuery, batchSize);
	}
}
