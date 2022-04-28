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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.MigrationVersion;

/**
 * A migration based on a catalog. The migration itself can contain a (partial) catalog with items that will be added
 * to the {@link MigrationContext Migration contexts} global catalog. Items with the same ids in newer migrations will
 * be added to the catalog. They will be picked up by operations depending on which migration the operation is applied.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class CatalogBasedMigration implements Migration {

	@Override
	public MigrationVersion getVersion() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getSource() {
		return null;
	}

	@Override public void apply(MigrationContext context) {

	}
}
