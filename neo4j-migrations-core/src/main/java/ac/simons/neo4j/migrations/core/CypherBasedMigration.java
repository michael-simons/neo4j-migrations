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
package ac.simons.neo4j.migrations.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A cypher script based migration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
final class CypherBasedMigration implements Migration {

	private final CypherResource cypherResource;
	private final String description;
	private final MigrationVersion version;
	/**
	 * In case a migration with the {@link #getSource() source} with different preconditions is available, we treat those as alternatives
	 */
	private List<String> alternativeChecksums;

	CypherBasedMigration(URL url) {
		this(url, Defaults.AUTOCRLF);
	}

	CypherBasedMigration(URL url, boolean autocrlf) {

		this.cypherResource = new CypherResource(url, autocrlf);
		this.version = MigrationVersion.parse(this.cypherResource.getScript());
		this.description = this.version.getDescription();
	}

	@Override
	public MigrationVersion getVersion() {
		return version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getSource() {
		return this.cypherResource.getScript();
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.of(cypherResource.getChecksum());
	}

	List<String> getAlternativeChecksums() {
		return Collections.unmodifiableList(alternativeChecksums);
	}

	void setAlternativeChecksums(List<String> alternativeChecksums) {
		this.alternativeChecksums = new ArrayList<>(alternativeChecksums);
	}

	@Override
	public void apply(MigrationContext context) throws MigrationsException {
		cypherResource.executeIn(context, UnaryOperator.identity());
	}

	List<String> getStatements() {
		return this.cypherResource.getStatements();
	}

	List<Precondition> getPreconditions() {
		return cypherResource.getPreconditions();
	}
}
