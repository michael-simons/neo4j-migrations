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
import java.util.Objects;

/**
 * A cypher script based migration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
final class CypherBasedMigration extends AbstractCypherBasedMigration implements MigrationWithPreconditions {

	/**
	 * In case a migration with the {@link #getSource() source} with different preconditions is available, we treat those as alternatives
	 */
	private List<String> alternativeChecksums = Collections.emptyList();

	CypherBasedMigration(URL url) {
		this(url, Defaults.AUTOCRLF);
	}

	CypherBasedMigration(URL url, boolean autocrlf) {
		super(CypherResource.of(url, autocrlf));
	}

	@Override
	public List<String> getAlternativeChecksums() {
		return Collections.unmodifiableList(alternativeChecksums);
	}

	@Override
	public void setAlternativeChecksums(List<String> alternativeChecksums) {

		Objects.requireNonNull(alternativeChecksums);
		this.alternativeChecksums = new ArrayList<>(alternativeChecksums);
	}

	@Override
	public List<Precondition> getPreconditions() {
		return Precondition.in(cypherResource);
	}
}
