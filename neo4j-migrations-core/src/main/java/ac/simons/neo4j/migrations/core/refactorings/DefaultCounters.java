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

import org.neo4j.driver.summary.SummaryCounters;

/**
 * Implementation of counters abstracting away over driver counters.
 *
 * @param nodesCreated number of nodes created
 * @param nodesDeleted number of nodes deleted
 * @param labelsAdded number of labels added
 * @param labelsRemoved number of labels removed
 * @param typesAdded number of types added
 * @param typesRemoved number of types removed
 * @param propertiesSet number of properties set
 * @param indexesAdded number of indexes added
 * @param indexesRemoved number of indexes removed
 * @param constraintsAdded number of constraints added
 * @param constraintsRemoved number of constraints removed
 * @author Michael J. Simons
 * @since 1.10.0
 */
record DefaultCounters(int nodesCreated, int nodesDeleted, int labelsAdded, int labelsRemoved, int typesAdded,
		int typesRemoved, int propertiesSet, int indexesAdded, int indexesRemoved, int constraintsAdded,
		int constraintsRemoved) implements Counters {

	DefaultCounters(SummaryCounters counters) {
		this(counters.nodesCreated(), counters.nodesDeleted(), counters.labelsAdded(), counters.labelsRemoved(),
				counters.relationshipsCreated(), counters.relationshipsDeleted(), counters.propertiesSet(),
				counters.indexesAdded(), counters.indexesRemoved(), counters.constraintsAdded(),
				counters.constraintsRemoved());
	}

	@Override
	public Counters add(Counters rhs) {

		if (rhs == Counters.Empty.INSTANCE) {
			return this;
		}
		return new DefaultCounters(this.nodesCreated + rhs.nodesCreated(), this.nodesDeleted + rhs.nodesDeleted(),
				this.labelsAdded + rhs.labelsAdded(), this.labelsRemoved + rhs.labelsRemoved(),
				this.typesAdded + rhs.typesAdded(), this.typesRemoved + rhs.typesRemoved(),
				this.propertiesSet + rhs.propertiesSet(), this.indexesAdded + rhs.indexesAdded(),
				this.indexesRemoved + rhs.indexesRemoved(), this.constraintsAdded + rhs.constraintsAdded(),
				this.constraintsRemoved + rhs.constraintsRemoved());
	}
}
