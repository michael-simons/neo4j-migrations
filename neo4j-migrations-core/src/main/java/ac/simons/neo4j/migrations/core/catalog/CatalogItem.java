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

/**
 * An item in the catalog (of either a single migration or the whole context with the merged catalog).
 *
 * @author Michael J. Simons
 * @param <T> The concrete type of this item, either a constraint or an index.
 * @soundtrack Anthrax - Spreading The Disease
 * @since 1.7.0
 */
public sealed interface CatalogItem<T extends ItemType> permits AbstractCatalogItem {

	/**
	 * {@return A unique name for a catalog item}
	 */
	Name getName();

	/**
	 * {@return Type information for the given item, specialized to the item type itself}
	 */
	T getType();

	/**
	 * Returns the {@literal true} if this item is equivalent to {@code that} item.
	 * @param that The other item to check
	 * @return {@literal true} if this item is equivalent to {@code that} item
	 */
	default boolean isEquivalentTo(CatalogItem<?> that) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@return {@literal true} if this item has a generated name}
	 */
	boolean hasGeneratedName();

	/**
	 * Creates a copy of this item with the specific name. Will return {@literal this} instance if the name has not
	 * changed.
	 *
	 * @param name The new name to use
	 * @return A (potentially) new item
	 * @since 1.13.0
	 */
	default CatalogItem<T> withName(String name) {
		throw new UnsupportedOperationException();
	}
}
