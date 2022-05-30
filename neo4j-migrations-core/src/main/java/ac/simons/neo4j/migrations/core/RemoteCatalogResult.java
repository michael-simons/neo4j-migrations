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
import ac.simons.neo4j.migrations.core.catalog.CatalogDiff;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;

import java.util.Collection;
import java.util.Optional;

/**
 * The result of retrieving the catalog of the current targeted database.
 *
 * @author Michael J. Simons
 * @soundtrack Motörhead - Snake Bite Love
 * @since TBA
 */
public final class RemoteCatalogResult implements DatabaseOperationResult, CatalogResult {

	private final String affectedDatabase;

	private final Catalog catalog;
	private final CatalogDiff diff;

	RemoteCatalogResult(Optional<String> affectedDatabase, Catalog catalog, Catalog localCatalog) {

		this.affectedDatabase = affectedDatabase.orElse(null);
		this.catalog = catalog;
		this.diff = CatalogDiff.between(catalog, localCatalog);
	}

	@Override
	public Catalog getValue() {
		return catalog;
	}

	/**
	 * @return a collection of items available only in the affected database.
	 */
	public Collection<CatalogItem<?>> getDatabaseOnlyItems() {
		return diff.getItemsOnlyInLeft();
	}

	/**
	 * @return a collection of items avaible only in the local catalog
	 */
	public Collection<CatalogItem<?>> getLocalOnlyItems() {
		return diff.getItemsOnlyInRight();
	}

	/**
	 * @return a collection of items that are equivalent but not identical in the calogs
	 */
	public Collection<CatalogItem<?>> getEquivalentItems() {
		return diff.getEquivalentItems();
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(this.affectedDatabase);
	}

	@Override
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder();
		sb.append("Catalog for the ")
			.append(getAffectedDatabase().map(database -> "database `" + database + "`").orElse("default database"))
			.append(" with ")
			.append(getValue().getItems().size())
			.append(" item(s)");
		if (diff.identical()) {
			sb.append(" is identical to the local catalog.");
		} else if (diff.equivalent()) {
			sb.append(" is equivalent to the local catalog.");
		} else {
			sb.append(" contains ")
				.append(getDatabaseOnlyItems().size())
				.append(" item(s) not in the local catalog and misses ")
				.append(getLocalOnlyItems().size())
				.append(" item(s) of the local catalog.");
		}
		return sb.toString();
	}
}
