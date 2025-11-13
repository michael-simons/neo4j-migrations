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
package ac.simons.neo4j.migrations.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import org.neo4j.driver.Record;
import org.neo4j.driver.SimpleQueryRunner;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;

/**
 * Catalog based on the items discoverable inside a Neo4j instance at a given point in
 * time.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class DatabaseCatalog implements Catalog {

	private final Collection<CatalogItem<?>> items;

	private DatabaseCatalog(Set<CatalogItem<?>> items) {
		this.items = items;
	}

	static Catalog full(Neo4jVersion version, SimpleQueryRunner queryRunner) {
		return of(version, queryRunner, true, false);
	}

	static Catalog of(Neo4jVersion version, SimpleQueryRunner queryRunner, boolean readOptions) {
		return of(version, queryRunner, readOptions, true);
	}

	private static Catalog of(Neo4jVersion version, SimpleQueryRunner queryRunner, boolean readOptions,
			boolean filterInternalConstraints) {

		Set<CatalogItem<?>> items = new LinkedHashSet<>();
		Function<Record, MapAccessor> mapAccessorMapper = r -> readOptions ? r
				: new FilteredMapAccessor(r, Collections.singleton("options"));

		Predicate<Constraint> internalConstraints = c -> true;
		if (filterInternalConstraints) {
			internalConstraints = constraint -> Arrays.stream(MigrationsLock.REQUIRED_CONSTRAINTS)
				.noneMatch(constraint::isEquivalentTo);
			internalConstraints = internalConstraints.and(c -> !Migrations.UNIQUE_VERSION.isEquivalentTo(c));
		}
		queryRunner.run(version.getShowConstraints())
			.stream()
			.map(mapAccessorMapper)
			.map(Constraint::parse)
			.filter(internalConstraints)
			.forEach(items::add);

		Predicate<Index> internalIndexes = index -> index.getType() != Index.Type.LOOKUP
				&& index.getType() != Index.Type.CONSTRAINT_BACKING_INDEX;
		if (filterInternalConstraints) {
			internalIndexes = internalIndexes.and(i -> !Migrations.REPEATED_AT.isEquivalentTo(i));
		}
		queryRunner.run(version.getShowIndexes())
			.stream()
			.map(mapAccessorMapper)
			.map(Index::parse)
			.filter(internalIndexes)
			.forEach(items::add);

		return new DatabaseCatalog(items);
	}

	@Override
	public Collection<CatalogItem<?>> getItems() {
		return this.items;
	}

	/**
	 * A map accessor that can filter other map-accessors. It does not support iterating
	 * over elements.
	 */
	private static final class FilteredMapAccessor implements MapAccessor {

		private final MapAccessor delegate;

		private final Set<String> filteredKeys;

		FilteredMapAccessor(MapAccessor delegate, Set<String> filter) {
			this.delegate = delegate;
			this.filteredKeys = new HashSet<>();
			this.delegate.keys().forEach(this.filteredKeys::add);
			this.filteredKeys.removeAll(filter);
		}

		@Override
		public Iterable<String> keys() {
			return this.filteredKeys;
		}

		@Override
		public boolean containsKey(String key) {
			return this.filteredKeys.contains(key);
		}

		@Override
		public Value get(String key) {
			return this.filteredKeys.contains(key) ? this.delegate.get(key) : Values.NULL;
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterable<Value> values() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Iterable<T> values(Function<Value, T> mapFunction) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> asMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
			throw new UnsupportedOperationException();
		}

	}

}
