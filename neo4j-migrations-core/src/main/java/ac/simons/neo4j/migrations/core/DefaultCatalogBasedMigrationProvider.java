/*
 * Copyright 2020-2024 the original author or authors.
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
 * Default handler for xml resources.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
@SuppressWarnings("missing-explicit-ctor")
public final class DefaultCatalogBasedMigrationProvider implements ResourceBasedMigrationProvider {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override public String getExtension() {
		return "xml";
	}

	@Override
	public Collection<Migration> handle(ResourceContext ctx) {

		return Collections.singletonList(CatalogBasedMigration.from(ctx));
	}
}
