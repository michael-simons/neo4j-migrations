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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * The current implementation of {@link CatalogDiff}.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class CatalogDiffImpl implements CatalogDiff {

	private final boolean identical;

	private final boolean equivalent;

	private final Collection<CatalogItem<?>> itemsOnlyInLeft;

	private final Collection<CatalogItem<?>> itemsOnlyInRight;

	private final Collection<CatalogItem<?>> equivalentItems;

	CatalogDiffImpl() {
		this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
	}

	CatalogDiffImpl(Set<CatalogItem<?>> itemsOnlyInLeft, Set<CatalogItem<?>> itemsOnlyInRight,
			Set<CatalogItem<?>> equivalentItems) {

		this.itemsOnlyInLeft = itemsOnlyInLeft;
		this.itemsOnlyInRight = itemsOnlyInRight;
		this.equivalentItems = equivalentItems;

		this.identical = itemsOnlyInLeft.isEmpty() && itemsOnlyInRight.isEmpty();
		this.equivalent = this.identical || (this.equivalentItems.containsAll(itemsOnlyInLeft)
				&& this.equivalentItems.containsAll(itemsOnlyInRight));
	}

	@Override
	public boolean identical() {
		return this.identical;
	}

	@Override
	public boolean equivalent() {
		return this.equivalent;
	}

	@Override
	public Collection<CatalogItem<?>> getItemsOnlyInLeft() {
		return Collections.unmodifiableCollection(this.itemsOnlyInLeft);
	}

	@Override
	public Collection<CatalogItem<?>> getItemsOnlyInRight() {
		return Collections.unmodifiableCollection(this.itemsOnlyInRight);
	}

	@Override
	public Collection<CatalogItem<?>> getEquivalentItems() {
		return Collections.unmodifiableCollection(this.equivalentItems);
	}

}
