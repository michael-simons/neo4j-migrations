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
import java.util.List;

/**
 * A catalog that is local to a migration.
 *
 * @author Michael J. Simons
 * @soundtrack Carcass - Surgical Steel
 * @since 1.7.0
 */
final class CatalogImpl implements Catalog {

	private final List<CatalogItem<?>> items;

	CatalogImpl(Collection<CatalogItem<?>> items) {
		this.items = List.copyOf(items);
	}

	@Override
	public Collection<CatalogItem<?>> getItems() {
		return items;
	}
}
