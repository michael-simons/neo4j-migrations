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
import java.util.function.UnaryOperator;

/**
 * This is a utility class for every {@link Migration} that wants to provide Cypher to
 * Neo4j-Migrations (via an extension). For example this can be a format extension
 * extracting Cypher from various text formats. This base class uses a
 * {@link CypherResource} under the hood taking care of checksuming Cypher etc.
 *
 * @author Michael J. Simons
 * @since 1.8.0
 */
public abstract non-sealed class AbstractCypherBasedMigration implements Migration {

	/**
	 * The underlying resource representing the content of this migration.
	 */
	protected final CypherResource cypherResource;

	/**
	 * The version of this migration.
	 */
	protected final MigrationVersion version;

	/**
	 * Required to be called so that the {@link #cypherResource} gets initialized proper.
	 * @param cypherResource the resource to be dealt with
	 */
	protected AbstractCypherBasedMigration(CypherResource cypherResource) {
		this.cypherResource = cypherResource;
		this.version = MigrationVersion.parse(this.cypherResource.getIdentifier());
	}

	@Override
	public final MigrationVersion getVersion() {
		return this.version;
	}

	@Override
	public Optional<String> getOptionalDescription() {
		return this.version.getOptionalDescription();
	}

	/**
	 * This can be overridden to provide a better identifier for this source (for example
	 * an anchor in an adoc file). The {@link CypherResource cypher resource} itself must
	 * have an identifier that can be parsed into a {@link MigrationVersion}.
	 * @return an identifier for this migration.
	 */
	@Override
	public String getSource() {
		return this.cypherResource.getIdentifier();
	}

	@Override
	public final Optional<String> getChecksum() {
		return Optional.of(this.cypherResource.getChecksum());
	}

	@Override
	public final void apply(MigrationContext context) throws MigrationsException {
		DefaultCypherResource.executeIn(this.cypherResource, context, UnaryOperator.identity());
	}

	@Override
	public final boolean isRepeatable() {
		return getVersion().isRepeatable();
	}

}
