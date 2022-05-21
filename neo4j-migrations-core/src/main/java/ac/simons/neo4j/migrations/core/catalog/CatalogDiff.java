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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This represents a diff results between two catalogs. It offers a handful of convience methods to quickly check if two
 * catalogs are identical or at least have equivalent content.
 *
 * @author Michael J. Simons
 * @soundtrack Pet Shop Boys - Fundamental
 * @since TBA
 */
public interface CatalogDiff {

	static CatalogDiff between(Catalog left, Catalog right) {

		if (left.isEmpty() && right.isEmpty()) {
			return new CatalogDiffImpl();
		} else if (left.isEmpty() && !right.isEmpty()) {
			Set<CatalogItem<?>> itemsOnlyInRight = new HashSet<>(right.getItems());
			return new CatalogDiffImpl(Collections.emptySet(), itemsOnlyInRight, Collections.emptySet());
		} else if (!left.isEmpty() && right.isEmpty()) {
			Set<CatalogItem<?>> itemsOnlyInLeft = new HashSet<>(left.getItems());
			return new CatalogDiffImpl(itemsOnlyInLeft, Collections.emptySet(), Collections.emptySet());
		} else {
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
	}

	boolean identical();

	boolean equivalent();

	Collection<CatalogItem<?>> getItemsOnlyInLeft();

	Collection<CatalogItem<?>> getItemsOnlyInRight();

	Collection<CatalogItem<?>> getEquivalentItems();
}
