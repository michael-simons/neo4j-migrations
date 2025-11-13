/*
 * Copyright 2020-2025 the original author or authors.
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

/**
 * A marker interface that can be added to a {@link Catalog} making it writable.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
interface WriteableCatalog extends Catalog {

	/**
	 * Adds all entries of the given {@link Catalog catalog} received in a specific
	 * {@link MigrationVersion} throwing a {@link MigrationsException} if a given
	 * {@link ac.simons.neo4j.migrations.core.catalog.CatalogItem} is already present in
	 * that version.
	 * @param version the version in which the {@literal catalog} was received
	 * @param other the catalog to add
	 * @param reset use {@literal true} to reset the catalog in this version
	 */
	void addAll(MigrationVersion version, Catalog other, boolean reset);

}
