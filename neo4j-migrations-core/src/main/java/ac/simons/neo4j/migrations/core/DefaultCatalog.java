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
package ac.simons.neo4j.migrations.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;

/**
 * Default catalog implementation.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
class DefaultCatalog implements WriteableCatalog, VersionedCatalog {

	private final Map<Name, NavigableMap<MigrationVersion, CatalogItem<?>>> items;

	private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();

	private final Comparator<MigrationVersion> comparator;

	private final NavigableMap<MigrationVersion, VersionedCatalog> oldVersions;

	DefaultCatalog(Comparator<MigrationVersion> comparator) {
		this(comparator, new HashMap<>());
	}

	private DefaultCatalog(Comparator<MigrationVersion> comparator,
			Map<Name, NavigableMap<MigrationVersion, CatalogItem<?>>> items) {
		this.comparator = comparator;
		this.oldVersions = new TreeMap<>(this.comparator);
		this.items = items;
	}

	@Override
	public void addAll(MigrationVersion version, Catalog other, boolean reset) {

		WriteLock lock = this.locks.writeLock();
		try {
			lock.lock();

			if (reset) {
				if (this.oldVersions.containsKey(version)) {
					throw new IllegalArgumentException(
							"Catalog has been already reset at version " + version.getValue());
				}
				else if (containsVersion(version)) {
					throw new IllegalArgumentException(
							"Version " + version.getValue() + " has already been used in this catalog.");
				}
				this.oldVersions.put(version, new DefaultCatalog(this.comparator, new HashMap<>(this.items)));
				this.items.clear();
			}

			for (CatalogItem<?> item : other.getItems()) {
				NavigableMap<MigrationVersion, CatalogItem<?>> versionedItems = this.items
					.computeIfAbsent(item.getName(), k -> new TreeMap<>(this.comparator));
				if (versionedItems.containsKey(version)) {
					throw new MigrationsException(String.format(
							"A constraint with the name '%s' has already been added to this catalog under the version %s.",
							item.getName().getValue(), version.getValue()));
				}
				versionedItems.put(version, item);
			}
		}
		finally {
			lock.unlock();
		}
	}

	private boolean containsVersion(MigrationVersion version) {
		return this.items.entrySet().stream().flatMap(v -> v.getValue().keySet().stream()).anyMatch(version::equals);
	}

	private <T> T withReadLockGet(Supplier<T> s) {

		ReadLock lock = this.locks.readLock();
		try {
			lock.lock();
			return s.get();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	@SuppressWarnings({ "squid:S6204" }) // toList vs Collectors.collect
	public Collection<CatalogItem<?>> getItems() {

		return withReadLockGet(() -> this.items.values()
			.stream()
			.map(NavigableMap::lastEntry)
			.map(Map.Entry::getValue)
			.collect(Collectors.toList()));
	}

	@Override
	@SuppressWarnings({ "squid:S6204" }) // toList vs Collectors.collect
	public Collection<CatalogItem<?>> getItemsPriorTo(MigrationVersion version) {

		return withReadLockGet(() -> {
			Map.Entry<MigrationVersion, VersionedCatalog> oldEntry = this.oldVersions.ceilingEntry(version);
			if (oldEntry != null) {
				return oldEntry.getValue().getItemsPriorTo(version);
			}
			return this.items.values()
				.stream()
				.map(m -> Optional.ofNullable(m.lowerEntry(version)).map(Map.Entry::getValue))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		});
	}

	@Override
	public Optional<CatalogItem<?>> getItemPriorTo(Name name, MigrationVersion version) {

		return withReadLockGet(() -> {
			Map.Entry<MigrationVersion, VersionedCatalog> oldEntry = this.oldVersions.ceilingEntry(version);
			if (oldEntry != null) {
				return oldEntry.getValue().getItemPriorTo(name, version);
			}
			return Optional.ofNullable(this.items.get(name)).map(m -> m.lowerEntry(version)).map(Map.Entry::getValue);
		});
	}

	@Override
	@SuppressWarnings({ "squid:S6204" }) // toList vs Collectors.collect
	public Collection<CatalogItem<?>> getItems(MigrationVersion version) {

		return withReadLockGet(() -> {
			Map.Entry<MigrationVersion, VersionedCatalog> oldEntry = this.oldVersions.higherEntry(version);
			if (oldEntry != null) {
				return oldEntry.getValue().getItems(version);
			}
			return this.items.values()
				.stream()
				.map(m -> Optional.ofNullable(m.floorEntry(version)).map(Map.Entry::getValue))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		});
	}

	@Override
	public Optional<CatalogItem<?>> getItem(Name name, MigrationVersion version) {

		return withReadLockGet(() -> {
			Map.Entry<MigrationVersion, VersionedCatalog> oldEntry = this.oldVersions.higherEntry(version);
			if (oldEntry != null) {
				return oldEntry.getValue().getItem(name, version);
			}
			return Optional.ofNullable(this.items.get(name)).map(m -> m.floorEntry(version)).map(Map.Entry::getValue);
		});
	}

}
