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
package ac.simons.neo4j.migrations.core;

/**
 * A {@link  RepairmentResult} will  be available  after a  successful attempt of  repairing a  database. An  attempt is
 * considered successful no repairing  was necessary or all errors in the remote  {@link MigrationChain migration chain}
 * could be fixed.
 *
 * @author Michael J. Simons
 * @soundtrack Deichkind - Neues vom Dauerzustand
 * @since TBA
 */
public final class RepairmentResult extends AbstractRepairmentResult {

	/**
	 * The outcome of an attempt of repairing a {@link MigrationChain migration chain}.
	 */
	public enum Outcome {

		/**
		 * This outcome indicates that no repairment was necessary. This is the case for example when no migrations have
		 * been applied yet to the target database.
		 */
		NO_REPAIRMENT_NECESSARY,

		/**
		 * This outcome indicates the database has been repaired and changes have been made. Check the additional data.
		 */
		REPAIRED
	}

	private final Outcome outcome;

	RepairmentResult(String affectedDatabase, long nodesDeleted, long nodesCreated, long relationshipsDeleted, long relationshipsCreated, long propertiesSet, Outcome outcome) {
		super(affectedDatabase, nodesDeleted, nodesCreated, relationshipsDeleted, relationshipsCreated, propertiesSet);
		this.outcome = outcome;
	}

	/**
	 * @return The outcome of the repairment attempt.
	 */
	public Outcome getOutcome() {
		return outcome;
	}

	@Override
	public String prettyPrint() {
		throw new UnsupportedOperationException("TODO");
	}

}
