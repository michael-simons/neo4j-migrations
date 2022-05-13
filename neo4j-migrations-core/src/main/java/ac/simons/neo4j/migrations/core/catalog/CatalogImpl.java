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
import java.util.Collections;
import java.util.List;

/**
 * A catalog that is local to a migration.
 *
 * @author Michael J. Simons
 * @soundtrack Carcass - Surgical Steel
 * @since TBA
 */
final class CatalogImpl implements Catalog {

	private final List<CatalogItem<?>> constraints;

	CatalogImpl(List<Constraint> constraints) {
		this.constraints = Collections.unmodifiableList(new ArrayList<>(constraints));
	}

	@Override
	public List<CatalogItem<?>> getItems() {
		return constraints;
	}
}
