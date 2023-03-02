package ac.simons.neo4j.migrations.core;

import java.util.Optional;

/**
 * Contains shared state for repair results (deleting migrations is considered a repairing attempt as well).
 *
 * @author Michael J. Simons
 * @since TBA
 */
abstract sealed class AbstractRepairmentResult implements DatabaseOperationResult
	permits DeleteResult, RepairmentResult {

	private final String affectedDatabase;

	private final long nodesDeleted;

	private final long relationshipsDeleted;

	private final long relationshipsCreated;

	private final long propertiesSet;

	AbstractRepairmentResult(String affectedDatabase, long nodesDeleted, long relationshipsDeleted, long relationshipsCreated, long propertiesSet) {
		this.affectedDatabase = affectedDatabase;
		this.nodesDeleted = nodesDeleted;
		this.relationshipsDeleted = relationshipsDeleted;
		this.relationshipsCreated = relationshipsCreated;
		this.propertiesSet = propertiesSet;
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(affectedDatabase);
	}

	/**
	 * @return how many nodes have been deleted.
	 */
	public long getNodesDeleted() {
		return nodesDeleted;
	}

	/**
	 * @return how many relationships have been deleted
	 */
	public long getRelationshipsDeleted() {
		return relationshipsDeleted;
	}

	/**
	 * @return how many relationships have been created
	 */
	public long getRelationshipsCreated() {
		return relationshipsCreated;
	}

	/**
	 * @return the number of properties set
	 */
	public long getPropertiesSet() {
		return propertiesSet;
	}
}
