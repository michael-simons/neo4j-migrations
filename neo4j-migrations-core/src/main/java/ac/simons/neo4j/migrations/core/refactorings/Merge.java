/*
 * Copyright 2020-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Refactoring that allows merging of entities.
 * @author Michael J. Simons
 * @since 1.10.0
 */
public sealed interface Merge extends Refactoring permits DefaultMerge {

	/**
	 * A {@link PropertyMergePolicy property merge policy} defines how properties with the same name defined on several
	 * nodes to be merged should be handled. A policy consists of a pattern that matches the names of properties and a
	 * strategy how to deal with them.
	 */
	class PropertyMergePolicy {

		/**
		 * The strategy how to deal with multiple properties of the same name.
		 */
		public enum Strategy {
			/**
			 * Keep all of them and collect them into an array on the target node. An array will also be created
			 * if only a single property exists on all matched nodes.
			 */
			KEEP_ALL,
			/**
			 * Keep only the first one. When using this strategy, you should make sure you order the nodes you want to merge precisely.
			 */
			KEEP_FIRST,
			/**
			 * Keep only the last one. When using this strategy, you should make sure you order the nodes you want to merge precisely.
			 */
			KEEP_LAST
		}

		/**
		 * Create a new policy.
		 * @param pattern The pattern that should match the property name
		 * @param strategy A strategy.
		 * @return The new policy.
		 */
		public static PropertyMergePolicy of(String pattern, Strategy strategy) {
			return new PropertyMergePolicy(Objects.requireNonNull(pattern), Objects.requireNonNull(strategy));
		}

		private final Pattern pattern;

		private final Strategy strategy;

		private PropertyMergePolicy(String pattern, Strategy strategy) {
			this.pattern = Pattern.compile(pattern);
			this.strategy = strategy;
		}

		/**
		 * @return A strategy describing who to deal with any property that matched this policy
		 */
		public Strategy strategy() {
			return strategy;
		}

		/**
		 * @return The pattern that is used to check if a graph property matches this policy
		 */
		public Pattern pattern() {
			return pattern;
		}

		Object apply(List<Object> values) {
			if (values.isEmpty()) {
				return null;
			}
			return switch (this.strategy) {
				case KEEP_ALL -> values;
				case KEEP_FIRST -> values.get(0);
				case KEEP_LAST -> values.get(values.size() - 1);
			};
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			PropertyMergePolicy that = (PropertyMergePolicy) o;
			return pattern.pattern().equals(that.pattern.pattern()) && strategy == that.strategy;
		}

		@Override
		public int hashCode() {
			return Objects.hash(pattern, strategy);
		}
	}

	/**
	 * Provides a refactoring merging a set of nodes. The custom query must return nodes as one node per record. The returned
	 * refactoring won't provide any policies how do deal with duplicate properties. If you are in doubt, specify one via
	 * {@link #nodes(String, List)}
	 *
	 * @param sourceQuery The source query identifying the set of nodes to be merged.
	 * @return The refactoring ready to use
	 */
	static Merge nodes(String sourceQuery) {
		return new DefaultMerge(sourceQuery, Collections.emptyList());
	}

	/**
	 * Provides a refactoring merging a set of nodes. The custom query must return nodes as one node per record.
	 *
	 * @param sourceQuery   The source query identifying the set of nodes to be merged.
	 * @param mergePolicies The policies that describe how to deal with duplicated properties.
	 * @return The refactoring ready to use
	 */
	static Merge nodes(String sourceQuery, List<PropertyMergePolicy> mergePolicies) {
		return new DefaultMerge(sourceQuery, new ArrayList<>(Objects.requireNonNull(mergePolicies)));
	}
}
