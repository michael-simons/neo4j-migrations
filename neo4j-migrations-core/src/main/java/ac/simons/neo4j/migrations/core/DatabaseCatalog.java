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
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.driver.QueryRunner;

/**
 * Catalog based on the items discoverable inside a Neo4j instance at a given point in time.
 *
 * @author Michael J. Simons
 * @soundtrack The Prodigy - Music For The Jilted Generation
 * @since TBA
 */
final class DatabaseCatalog implements Catalog {

	static Catalog of(Neo4jVersion version, QueryRunner queryRunner) {
		Set<CatalogItem<?>> items = new HashSet<>();

		queryRunner.run(version.getShowConstraints())
			.stream()
			.map(Constraint::parse)
			.filter(constraint -> Arrays.stream(MigrationsLock.REQUIRED_CONSTRAINTS).noneMatch(constraint::isEquivalentTo))
			.filter(constraint -> !Migrations.UNIQUE_VERSION.isEquivalentTo(constraint))
			.forEach(items::add);

		queryRunner.run(version.getShowIndexes())
			.stream()
			.map(Index::parse)
			.filter(index -> index.getType() != Index.Type.LOOKUP)
			.forEach(items::add);

		return new DatabaseCatalog(items);
	}

	private final Set<CatalogItem<?>> items;

	private DatabaseCatalog(Set<CatalogItem<?>> items) {
		this.items = Collections.unmodifiableSet(items);
	}

	@Override
	public Collection<CatalogItem<?>> getItems() {
		return items;
	}
}
