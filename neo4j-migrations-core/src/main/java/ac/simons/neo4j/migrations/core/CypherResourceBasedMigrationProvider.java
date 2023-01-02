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

import java.util.Collection;
import java.util.Collections;

/**
 * Default handler for cypher resources.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class CypherResourceBasedMigrationProvider implements ResourceBasedMigrationProvider {

	/**
	 * Save to call, but discouraged.
	 */
	@SuppressWarnings("squid:S1186")
	// Public constructor required by the service loader, cannot be moved to an internal package due to dependencies of the core package
	public CypherResourceBasedMigrationProvider() {
	}

	@Override
	public String getExtension() {
		return Defaults.CYPHER_SCRIPT_EXTENSION;
	}

	@Override
	public Collection<Migration> handle(ResourceContext ctx) {
		return Collections.singletonList(new CypherBasedMigration(ctx.getUrl(), ctx.getConfig().isAutocrlf()));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
