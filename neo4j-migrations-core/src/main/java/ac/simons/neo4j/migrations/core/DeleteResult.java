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

import java.util.Optional;

/**
 * A  {@link DeleteResult}  will be  created after  using {@link  Migrations#delete(MigrationVersion)} for  deleting one
 * single migration. It  contains the number of deleted nodes  and relationships as well as the  number of newly created
 * relationships.
 *
 * @author Michael J. Simons
 * @since TBA
 */
public final class DeleteResult implements DatabaseOperationResult {

	private final String affectedDatabase;

	private final MigrationVersion version;

	private final long nodesDeleted;

	private final long relationshipsDeleted;

	private final long relationshipsCreated;

	DeleteResult(String affectedDatabase, MigrationVersion version, long nodesDeleted, long relationshipsDeleted, long relationshipsCreated) {
		this.affectedDatabase = affectedDatabase;
		this.version = version;
		this.nodesDeleted = nodesDeleted;
		this.relationshipsDeleted = relationshipsDeleted;
		this.relationshipsCreated = relationshipsCreated;
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(affectedDatabase);
	}

	/**
	 * @return {@literal true} if the database has been changed
	 */
	public boolean isDatabaseChanged() {
		return version != null && nodesDeleted > 0;
	}

	/**
	 * @return the deleted version (if any)
	 */
	public Optional<MigrationVersion> getVersion() {
		return Optional.ofNullable(version);
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

	@Override
	public String prettyPrint() {

		if (!isDatabaseChanged()) {
			return "Database is unchanged, no version has been deleted.";
		}
		return String.format(
			"Migration %s has been removed (deleted %d nodes and %d relationships from %s).",
			toString(this.version),
			this.getNodesDeleted(),
			this.getRelationshipsDeleted(),
			this.getAffectedDatabase().map(v -> "`" + v + "`").orElse("the default database")
		);
	}

	static String toString(MigrationVersion version) {

		return version.getValue()
			+ version.getOptionalDescription().map(d -> String.format(" (\"%s\")", d)).orElse("");
	}
}
