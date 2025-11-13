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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This represents a diff results between two catalogs. It offers a handful of convince
 * methods to quickly check if two catalogs are identical or at least have equivalent
 * content.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public interface CatalogDiff {

	/**
	 * Creates a diff between to catalogs.
	 * @param left left catalog
	 * @param right right catalog
	 * @return a diff on the given catalogs
	 */
	static CatalogDiff between(Catalog left, Catalog right) {

		if (left.isEmpty() && right.isEmpty()) {
			return new CatalogDiffImpl();
		}
		else if (left.isEmpty() && !right.isEmpty()) {
			Set<CatalogItem<?>> itemsOnlyInRight = new HashSet<>(right.getItems());
			return new CatalogDiffImpl(Collections.emptySet(), itemsOnlyInRight, Collections.emptySet());
		}
		else if (!left.isEmpty() && right.isEmpty()) {
			Set<CatalogItem<?>> itemsOnlyInLeft = new HashSet<>(left.getItems());
			return new CatalogDiffImpl(itemsOnlyInLeft, Collections.emptySet(), Collections.emptySet());
		}

		Set<CatalogItem<?>> itemsOnlyInLeft = new HashSet<>(left.getItems());
		Set<CatalogItem<?>> itemsOnlyInRight = new HashSet<>(right.getItems());
		itemsOnlyInLeft.removeAll(right.getItems());
		itemsOnlyInRight.removeAll(left.getItems());

		Set<CatalogItem<?>> equivalentItems = new LinkedHashSet<>();
		for (CatalogItem<?> catalogItem : itemsOnlyInLeft) {
			if (right.getItems().stream().anyMatch(catalogItem::isEquivalentTo)) {
				equivalentItems.add(catalogItem);
			}
		}
		for (CatalogItem<?> catalogItem : itemsOnlyInRight) {
			if (left.getItems().stream().anyMatch(catalogItem::isEquivalentTo)) {
				equivalentItems.add(catalogItem);
			}
		}

		return new CatalogDiffImpl(itemsOnlyInLeft, itemsOnlyInRight, equivalentItems);
	}

	/**
	 * Returns the {@literal true} if the catalogs are identical, {@literal false}
	 * otherwise.
	 * @return {@literal true} if the catalogs are identical, {@literal false} otherwise
	 */
	boolean identical();

	/**
	 * Will always return {@literal true} when {@link #identical()} returns
	 * {@literal true}.
	 * @return {@literal true} if the catalogs are equivalent, {@literal false} otherwise
	 */
	boolean equivalent();

	/**
	 * Will always return an empty collection when {@link #identical()} returns
	 * {@literal true}.
	 * @return a collection of items that are only in the left catalog
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	Collection<CatalogItem<?>> getItemsOnlyInLeft();

	/**
	 * Will always return an empty collection when {@link #identical()} returns
	 * {@literal true}.
	 * @return a collection of items that are only in the right catalog
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	Collection<CatalogItem<?>> getItemsOnlyInRight();

	/**
	 * Will always return an empty collection when {@link #identical()} returns
	 * {@literal true}.
	 * @return a collection of equivalent items
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	Collection<CatalogItem<?>> getEquivalentItems();

}
