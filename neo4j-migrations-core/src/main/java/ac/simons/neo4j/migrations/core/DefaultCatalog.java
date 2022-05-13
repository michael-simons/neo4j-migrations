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
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
class DefaultCatalog implements WriteableCatalog, VersionedCatalog {

	private final Map<Name, NavigableMap<MigrationVersion, CatalogItem<?>>> items = new HashMap<>();
	private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();

	@Override
	public void addAll(MigrationVersion version, Catalog other) {

		WriteLock lock = locks.writeLock();
		try {
			lock.lock();
			for (CatalogItem<?> item : other.getItems()) {
				NavigableMap<MigrationVersion, CatalogItem<?>> versionedItems = items.computeIfAbsent(
					item.getName(), k -> new TreeMap<>(new MigrationVersion.VersionComparator()));
				if (versionedItems.containsKey(version)) {
					throw new MigrationsException(String.format(
						"A constraint with the name '%s' has already been added to this catalog under the version %s.",
						item.getName().getValue(), version.getValue()));
				}
				versionedItems.put(version, item);
			}
		} finally {
			lock.unlock();
		}
	}

	<T> T withReadLockGet(Supplier<T> s) {

		ReadLock lock = locks.readLock();
		try {
			lock.lock();
			return s.get();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public List<CatalogItem<?>> getItems() {

		return withReadLockGet(() -> items.values().stream().map(NavigableMap::lastEntry)
			.map(Map.Entry::getValue)
			.collect(Collectors.toList()));
	}

	@Override
	public List<CatalogItem<?>> getItemsPriorTo(MigrationVersion version) {

		return withReadLockGet(() -> items.values().stream()
			.map(m -> Optional.ofNullable(m.lowerEntry(version)).map(Map.Entry::getValue))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList()));
	}

	@Override
	public Optional<CatalogItem<?>> getItemPriorTo(Name name, MigrationVersion version) {

		return withReadLockGet(() -> Optional.ofNullable(items.get(name))
			.map(m -> m.lowerEntry(version))
			.map(Map.Entry::getValue));
	}

	@Override
	public List<CatalogItem<?>> getItems(MigrationVersion version) {

		return withReadLockGet(() -> items.values().stream()
			.map(m -> Optional.ofNullable(m.floorEntry(version)).map(Map.Entry::getValue))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList()));
	}

	@Override
	public Optional<CatalogItem<?>> getItem(Name name, MigrationVersion version) {

		return Optional.ofNullable(items.get(name))
			.map(m -> m.floorEntry(version))
			.map(Map.Entry::getValue);
	}

}
