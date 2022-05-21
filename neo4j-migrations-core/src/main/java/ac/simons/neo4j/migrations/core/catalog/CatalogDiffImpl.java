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
import java.util.List;

/**
 * @author Michael J. Simons
 * @soundtrack Pet Shop Boys - Fundamental
 * @since TBA
 */
final class CatalogDiffImpl implements CatalogDiff {

	private final boolean identical;

	private final boolean equivalent;

	private final List<CatalogItem<?>> itemsOnlyInLeft;

	private final List<CatalogItem<?>> itemsOnlyInRight;

	private final List<List<CatalogItem<?>>> equivalentItemsWithDifferentNames;

	CatalogDiffImpl(boolean identical, boolean equivalent) {
		this.identical = identical;
		this.equivalent = equivalent;
		this.itemsOnlyInLeft = Collections.emptyList();
		this.itemsOnlyInRight = Collections.emptyList();
		this.equivalentItemsWithDifferentNames = Collections.emptyList();
	}

	CatalogDiffImpl(boolean identical, boolean equivalent,
		List<CatalogItem<?>> itemsOnlyInLeft,
		List<CatalogItem<?>> itemsOnlyInRight,
		List<List<CatalogItem<?>>> equivalentItemsWithDifferentNames) {
		this.identical = identical;
		this.equivalent = equivalent;
		this.itemsOnlyInLeft = itemsOnlyInLeft;
		this.itemsOnlyInRight = itemsOnlyInRight;
		this.equivalentItemsWithDifferentNames = equivalentItemsWithDifferentNames;
	}

	@Override
	public boolean identical() {
		return identical;
	}

	@Override
	public boolean equivalent() {
		return equivalent;
	}

	@Override
	public Collection<CatalogItem<?>> getItemsOnlyInLeft() {
		return Collections.unmodifiableList(this.itemsOnlyInLeft);
	}

	@Override
	public Collection<CatalogItem<?>> getItemsOnlyInRight() {
		return Collections.unmodifiableList(this.itemsOnlyInRight);
	}
}
