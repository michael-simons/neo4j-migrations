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

import ac.simons.neo4j.migrations.core.MigrationVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a catalog of
 * <ul>
 *     <li>Constraints</li>
 *     <li>Indizes</li>
 * </ul>
 * and probably other things in the future to come. The complete catalog of a {@link ac.simons.neo4j.migrations.core.MigrationContext migration context}
 * will be made up of all catalog entries found in the migrations discovered. Catalog entries of the same type and with the
 * same id will all live up in the corresponding bucket so that for example a drop operation can refer to an older version
 * of a constraint to be dropped, either explicitly or implicit.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
final class Catalog {

	private final Map<CatalogId, Constraint> constraints = new HashMap<>();

	public synchronized void addAll(MigrationVersion version, List<Constraint> newConstraints) {

		newConstraints.forEach(constraint -> {
			CatalogId id = new CatalogId(constraint.getName(), version);
			constraints.put(id, constraint);
		});
	}
}
