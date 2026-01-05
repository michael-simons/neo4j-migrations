/*
 * Copyright 2020-2026 the original author or authors.
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
package ac.simons.neo4j.migrations.core;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Result of a clean operation. The result is immutable.
 *
 * @author Michael J. Simons
 * @since 1.1.0
 */
public final class CleanResult implements DatabaseOperationResult {

	private final String affectedDatabase;

	private final List<String> chainsDeleted;

	private final long nodesDeleted;

	private final long relationshipsDeleted;

	private final long constraintsRemoved;

	private final long indexesRemoved;

	CleanResult(Optional<String> affectedDatabase, List<String> chainsDeleted, long nodesDeleted,
			long relationshipsDeleted, long constraintsRemoved, long indexesRemoved) {
		this.affectedDatabase = affectedDatabase.orElse(null);
		this.chainsDeleted = List.copyOf(chainsDeleted);
		this.nodesDeleted = nodesDeleted;
		this.relationshipsDeleted = relationshipsDeleted;
		this.constraintsRemoved = constraintsRemoved;
		this.indexesRemoved = indexesRemoved;
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(this.affectedDatabase);
	}

	/**
	 * The list of chains deleted.
	 * @return the name of the chains' migration targets
	 */
	public List<String> getChainsDeleted() {
		return this.chainsDeleted;
	}

	/**
	 * {@return how many nodes have been deleted}
	 */
	public long getNodesDeleted() {
		return this.nodesDeleted;
	}

	/**
	 * {@return how many relationships have been deleted}
	 */
	public long getRelationshipsDeleted() {
		return this.relationshipsDeleted;
	}

	/**
	 * {@return how many constraints have been removed}
	 */
	public long getConstraintsRemoved() {
		return this.constraintsRemoved;
	}

	/**
	 * {@return how many indexes have been removed}
	 */
	public long getIndexesRemoved() {
		return this.indexesRemoved;
	}

	@Override
	public String prettyPrint() {

		String prefix = "chain" + ((getChainsDeleted().size() > 1) ? "s " : " ");
		return String.format("Deleted %s (%d nodes and %d relationships in total) and %d constraints from %s.",
				getChainsDeleted().isEmpty() ? "no chains"
						: getChainsDeleted().stream().collect(Collectors.joining(", ", prefix, "")),
				this.getNodesDeleted(), this.getRelationshipsDeleted(), this.getConstraintsRemoved(),
				this.getAffectedDatabase().map(v -> "`" + v + "`").orElse("the default database"));
	}

}
