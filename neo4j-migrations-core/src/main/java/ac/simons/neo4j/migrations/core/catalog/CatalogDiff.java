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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
			return new CatalogDiffImpl(true, true);
		} else if (left.isEmpty() && !right.isEmpty()) {
			List<CatalogItem<?>> itemsOnlyInRight = new ArrayList<>(right.getItems());
			return new CatalogDiffImpl(false, false, Collections.emptyList(), itemsOnlyInRight,
				Collections.emptyList());
		} else if (!left.isEmpty() && right.isEmpty()) {
			List<CatalogItem<?>> itemsOnlyInLeft = new ArrayList<>(left.getItems());
			return new CatalogDiffImpl(false, false, itemsOnlyInLeft, Collections.emptyList(), Collections.emptyList());
		} else {
			throw new IllegalStateException("not done yet");
		}
	}

	boolean identical();

	boolean equivalent();

	Collection<CatalogItem<?>> getItemsOnlyInLeft();

	Collection<CatalogItem<?>> getItemsOnlyInRight();
}
