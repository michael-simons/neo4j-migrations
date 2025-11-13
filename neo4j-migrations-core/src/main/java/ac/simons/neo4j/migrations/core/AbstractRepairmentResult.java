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
package ac.simons.neo4j.migrations.core;

import java.util.Optional;

/**
 * Contains shared state for repair results (deleting migrations is considered a repairing
 * attempt as well).
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
abstract sealed class AbstractRepairmentResult implements DatabaseOperationResult
		permits DeleteResult, RepairmentResult {

	private final String affectedDatabase;

	private final long nodesDeleted;

	private final long nodesCreated;

	private final long relationshipsDeleted;

	private final long relationshipsCreated;

	private final long propertiesSet;

	AbstractRepairmentResult(String affectedDatabase, long nodesDeleted, long nodesCreated, long relationshipsDeleted,
			long relationshipsCreated, long propertiesSet) {
		this.affectedDatabase = affectedDatabase;
		this.nodesDeleted = nodesDeleted;
		this.nodesCreated = nodesCreated;
		this.relationshipsDeleted = relationshipsDeleted;
		this.relationshipsCreated = relationshipsCreated;
		this.propertiesSet = propertiesSet;
	}

	static String toString(MigrationVersion version) {

		return version.getValue() + version.getOptionalDescription().map(d -> String.format(" (\"%s\")", d)).orElse("");
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(this.affectedDatabase);
	}

	/**
	 * {@return how many nodes have been deleted}
	 */
	long getNodesDeleted() {
		return this.nodesDeleted;
	}

	/**
	 * {@return how many nodes have been created}
	 */
	long getNodesCreated() {
		return this.nodesCreated;
	}

	/**
	 * {@return how many relationships have been deleted}
	 */
	long getRelationshipsDeleted() {
		return this.relationshipsDeleted;
	}

	/**
	 * {@return how many relationships have been created}
	 */
	long getRelationshipsCreated() {
		return this.relationshipsCreated;
	}

	/**
	 * {@return the number of properties set}
	 */
	long getPropertiesSet() {
		return this.propertiesSet;
	}

}
