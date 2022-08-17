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

import java.util.Map;

/**
 * Represents the available changes to the Graph done by a refactoring.
 *
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Adrenochrom
 * @since 1.10.0
 */
public interface Counters {

	/**
	 * @return An empty instance
	 */
	static Counters empty() {
		return Empty.INSTANCE;
	}

	/**
	 * Creates a new {@link Counters} instance from the values inside a map. If a required value is not present, {@literal 0}
	 * will be assumed.
	 *
	 * @param values The values to be used
	 * @return Counters, never {@literal null}
	 */
	static Counters of(Map<String, Integer> values) {
		return new DefaultCounters(
			values.getOrDefault("nodesCreated", 0),
			values.getOrDefault("nodesDeleted", 0),
			values.getOrDefault("labelsAdded", 0),
			values.getOrDefault("labelsRemoved", 0),
			values.getOrDefault("typesAdded", 0),
			values.getOrDefault("typesRemoved", 0),
			values.getOrDefault("propertiesSet", 0),
			values.getOrDefault("indexesAdded", 0),
			values.getOrDefault("indexesRemoved", 0),
			values.getOrDefault("constraintsAdded", 0),
			values.getOrDefault("constraintsRemoved", 0)
		);
	}

	/**
	 * @return Number of nodes created
	 */
	int nodesCreated();

	/**
	 * @return Number of nodes deleted
	 */
	int nodesDeleted();

	/**
	 * @return Number of labels added
	 */
	int labelsAdded();

	/**
	 * @return Number of labels removed
	 */
	int labelsRemoved();

	/**
	 * @return Number of types added
	 */
	int typesAdded();

	/**
	 * @return Number types removed
	 */
	int typesRemoved();

	/**
	 * @return Number of properties changed
	 */
	int propertiesSet();

	/**
	 * @return number of indexes added to the schema.
	 */
	int indexesAdded();

	/**
	 * @return number of indexes removed from the schema.
	 */
	int indexesRemoved();

	/**
	 * @return number of constraints added to the schema.
	 */
	int constraintsAdded();

	/**
	 * @return number of constraints removed from the schema.
	 */
	int constraintsRemoved();

	/**
	 * Add {@code rhs} to this instance, creating a new one
	 *
	 * @param rhs The counters to add to this instance
	 * @return a new set of counters
	 */
	Counters add(Counters rhs);

	/**
	 * An empty instance.
	 */
	enum Empty implements Counters {

		/**
		 * The single, empty instance.
		 */
		INSTANCE;

		@Override
		public int nodesCreated() {
			return 0;
		}

		@Override
		public int nodesDeleted() {
			return 0;
		}

		@Override
		public int labelsAdded() {
			return 0;
		}

		@Override
		public int labelsRemoved() {
			return 0;
		}

		@Override
		public int typesAdded() {
			return 0;
		}

		@Override
		public int typesRemoved() {
			return 0;
		}

		@Override
		public int propertiesSet() {
			return 0;
		}

		@Override
		public int indexesAdded() {
			return 0;
		}

		@Override
		public int indexesRemoved() {
			return 0;
		}

		@Override
		public int constraintsAdded() {
			return 0;
		}

		@Override
		public int constraintsRemoved() {
			return 0;
		}

		@Override
		public Counters add(Counters rhs) {

			if (rhs == INSTANCE) {
				return this;
			}
			return new DefaultCounters(
				rhs.nodesCreated(),
				rhs.nodesDeleted(),
				rhs.labelsAdded(),
				rhs.labelsRemoved(),
				rhs.typesAdded(),
				rhs.typesRemoved(),
				rhs.propertiesSet(),
				rhs.indexesAdded(),
				rhs.indexesRemoved(),
				rhs.constraintsAdded(),
				rhs.constraintsRemoved()
			);
		}
	}
}
