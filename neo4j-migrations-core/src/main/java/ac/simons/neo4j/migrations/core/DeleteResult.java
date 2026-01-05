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

import java.util.Optional;

/**
 * A {@link DeleteResult} will be created after using
 * {@link Migrations#delete(MigrationVersion)} for deleting one single migration. It
 * contains the number of deleted nodes and relationships as well as the number of newly
 * created relationships.
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
public final class DeleteResult extends AbstractRepairmentResult {

	private final MigrationVersion version;

	DeleteResult(String affectedDatabase, long nodesDeleted, long nodesCreated, long relationshipsDeleted,
			long relationshipsCreated, long propertiesSet, MigrationVersion version) {
		super(affectedDatabase, nodesDeleted, nodesCreated, relationshipsDeleted, relationshipsCreated, propertiesSet);
		this.version = version;
	}

	/**
	 * {@return true if the database has been changed}
	 */
	public boolean isDatabaseChanged() {
		return this.version != null && getNodesDeleted() > 0;
	}

	/**
	 * {@return the deleted version (if any)}
	 */
	public Optional<MigrationVersion> getVersion() {
		return Optional.ofNullable(this.version);
	}

	@Override
	public String prettyPrint() {

		if (!isDatabaseChanged()) {
			return "Database is unchanged, no version has been deleted.";
		}
		return String.format("Migration %s has been removed (deleted %d nodes and %d relationships from %s).",
				toString(this.version), this.getNodesDeleted(), this.getRelationshipsDeleted(),
				this.getAffectedDatabase().map(v -> "`" + v + "`").orElse("the default database"));
	}

}
