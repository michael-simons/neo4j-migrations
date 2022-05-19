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

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.util.Collection;
import java.util.Optional;

/**
 * An extended catalog keeping track of items as defined by {@link CatalogBasedMigration catalog based migrations} and their
 * {@link MigrationVersion version}.
 *
 * @author Michael J. Simons
 * @since TBA
 */
public interface VersionedCatalog extends Catalog {

	/**
	 * A list of all items prior to a given version.
	 *
	 * @param version The version up to but not including to retrieve items for
	 * @return a list of all items prior to the introduction of {@literal version}.
	 */
	Collection<CatalogItem<?>> getItemsPriorTo(MigrationVersion version);

	/**
	 * A single item prior to a given version. The result will be empty if the item has not yet been defined.
	 *
	 * @param name    The id of the item to retrieve
	 * @param version The version up to but not including to retrieve the constraint for
	 * @return an optional item as defined prior to the introduction of {@literal version}
	 */
	Optional<CatalogItem<?>> getItemPriorTo(Name name, MigrationVersion version);

	/**
	 * A list of all items up to and including a given version.
	 *
	 * @param version The version up to retrieve constraints for
	 * @return a list of all items up to the introduction of {@literal version}.
	 */
	Collection<CatalogItem<?>> getItems(MigrationVersion version);

	/**
	 * A single item for a given version. The result will be empty if the item has not yet been defined.
	 *
	 * @param name    The id of the item to retrieve
	 * @param version The version in which to retrieve the constraint for
	 * @return an optional item as defined with to the introduction of {@literal version}
	 */
	Optional<CatalogItem<?>> getItem(Name name, MigrationVersion version);
}
