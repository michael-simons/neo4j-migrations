/*
 * Copyright 2020-2021 the original author or authors.
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
import java.util.Optional;

/**
 * A cypher script based migration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
final class CypherBasedMigration extends AbstractCypherResource implements Migration {

	private final String description;
	private final MigrationVersion version;

	CypherBasedMigration(URL url) {
		this(url, Defaults.AUTOCRLF);
	}

	CypherBasedMigration(URL url, boolean autocrlf) {

		super(url, autocrlf);
		this.version = MigrationVersion.parse(this.script);
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
		return this.script;
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.of(getRequiredChecksum());
	}

	@Override
	public void apply(MigrationContext context) throws MigrationsException {
		super.executeIn(context);
	}

	@Override
	public String toString() {
		return "CypherBasedMigration{" +
			"url=" + url +
			", script='" + script + '\'' +
			'}';
	}
}
