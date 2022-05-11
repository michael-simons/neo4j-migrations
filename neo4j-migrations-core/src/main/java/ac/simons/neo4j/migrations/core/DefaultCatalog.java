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
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
class DefaultCatalog implements WriteableCatalog, VersionedCatalog {

	private final Map<Id, NavigableMap<MigrationVersion, Constraint>> constraints = new HashMap<>();

	@Override
	public void addAll(MigrationVersion version, Catalog other) {

		for (Constraint constraint : other.getConstraints()) {
			NavigableMap<MigrationVersion, Constraint> versionedItems = constraints.computeIfAbsent(
				constraint.getId(), k -> new TreeMap<>(new MigrationVersion.VersionComparator()));
			if (versionedItems.containsKey(version)) {
				throw new MigrationsException(String.format(
					"A constraint with the id '%s' has already been added to this catalog under the version %s.",
					constraint.getId().getValue(), version.getValue()));
			}
			versionedItems.put(version, constraint);
		}
	}

	@Override
	public List<Constraint> getConstraints() {
		return constraints.values().stream().map(NavigableMap::lastEntry).map(Map.Entry::getValue)
			.collect(Collectors.toList());
	}

	@Override
	public List<Constraint> getConstraintsPriorTo(MigrationVersion version) {

		return constraints.values().stream()
			.map(m -> Optional.ofNullable(m.lowerEntry(version)).map(Map.Entry::getValue))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	@Override
	public Optional<Constraint> getConstraintPriorTo(Id id, MigrationVersion version) {

		return Optional.ofNullable(constraints.get(id))
			.map(m -> m.lowerEntry(version))
			.map(Map.Entry::getValue);
	}

	@Override
	public List<Constraint> getConstraints(MigrationVersion version) {

		return constraints.values().stream()
			.map(m -> Optional.ofNullable(m.floorEntry(version)).map(Map.Entry::getValue))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	@Override
	public Optional<Constraint> getConstraint(Id id, MigrationVersion version) {

		return Optional.ofNullable(constraints.get(id))
			.map(m -> m.floorEntry(version))
			.map(Map.Entry::getValue);
	}

}
