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

import org.neo4j.driver.summary.SummaryCounters;

/**
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Adrenochrom
 * @since 1.10.0
 */
final class DefaultCounters implements Counters {

	private final int nodesCreated;

	private final int nodesDeleted;

	private final int labelsAdded;

	private final int labelsRemoved;

	private final int typesAdded;

	private final int typesRemoved;

	private final int propertiesSet;

	private final int indexesAdded;

	private final int indexesRemoved;

	private final int constraintsAdded;

	private final int constraintsRemoved;

	DefaultCounters(SummaryCounters counters) {
		this(counters.nodesCreated(), counters.nodesDeleted(), counters.labelsAdded(), counters.labelsRemoved(),
			counters.relationshipsCreated(),
			counters.relationshipsDeleted(), counters.propertiesSet(), counters.indexesAdded(),
			counters.indexesRemoved(), counters.constraintsAdded(), counters.constraintsRemoved());

	}

	@SuppressWarnings("squid:S107") // Yes, 11 arguments are ugly as hell, but it's saner than a map
	DefaultCounters(int nodesCreated, int nodesDeleted, int labelsAdded, int labelsRemoved,
		int typesAdded,
		int typesRemoved, int propertiesSet, int indexesAdded, int indexesRemoved, int constraintsAdded,
		int constraintsRemoved) {
		this.nodesCreated = nodesCreated;
		this.nodesDeleted = nodesDeleted;
		this.labelsAdded = labelsAdded;
		this.labelsRemoved = labelsRemoved;
		this.typesAdded = typesAdded;
		this.typesRemoved = typesRemoved;
		this.propertiesSet = propertiesSet;
		this.indexesAdded = indexesAdded;
		this.indexesRemoved = indexesRemoved;
		this.constraintsAdded = constraintsAdded;
		this.constraintsRemoved = constraintsRemoved;
	}

	@Override
	public int nodesCreated() {
		return nodesCreated;
	}

	@Override
	public int nodesDeleted() {
		return nodesDeleted;
	}

	@Override
	public int labelsAdded() {
		return labelsAdded;
	}

	@Override
	public int labelsRemoved() {
		return labelsRemoved;
	}

	@Override
	public int typesAdded() {
		return typesAdded;
	}

	@Override
	public int typesRemoved() {
		return typesRemoved;
	}

	@Override
	public int propertiesSet() {
		return propertiesSet;
	}

	@Override
	public int indexesAdded() {
		return indexesAdded;
	}

	@Override
	public int indexesRemoved() {
		return indexesRemoved;
	}

	@Override
	public int constraintsAdded() {
		return constraintsAdded;
	}

	@Override
	public int constraintsRemoved() {
		return constraintsRemoved;
	}

	@Override
	public Counters add(Counters rhs) {

		if (rhs == Counters.Empty.INSTANCE) {
			return this;
		}
		return new DefaultCounters(
			this.nodesCreated + rhs.nodesCreated(),
			this.nodesDeleted + rhs.nodesDeleted(),
			this.labelsAdded + rhs.labelsAdded(),
			this.labelsRemoved + rhs.labelsRemoved(),
			this.typesAdded + rhs.typesAdded(),
			this.typesRemoved + rhs.typesRemoved(),
			this.propertiesSet + rhs.propertiesSet(),
			this.indexesAdded + rhs.indexesAdded(),
			this.indexesRemoved + rhs.indexesRemoved(),
			this.constraintsAdded + rhs.constraintsAdded(),
			this.constraintsRemoved + rhs.constraintsRemoved()
		);
	}
}
