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
package ac.simons.neo4j.migrations.core.refactorings;

import ac.simons.neo4j.migrations.core.catalog.Index;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Migrates existing B-tree indexes and constraints backed by such indexes to Neo4j 5.0+ and higher supported indexes.
 * By default, all B-tree indexes will be migrated to {@literal RANGE} indexes, but this is configurable.
 *
 * @author Michael J. Simons
 * @soundtrack Bobby Fletcher &amp; Koljah - Vielleicht ist es besser so
 * @since TBA
 */
public interface MigrateBTreeIndexes extends Refactoring {

	/**
	 * The default suffix for constraints and indexes that are bound to replace old btree based constraints and indexes.
	 * The suffix will only be needed when old constraints and indexes are kept around not dropped.
	 */
	String DEFAULT_SUFFIX = "_new";

	/**
	 * Prepares migration of old B-tree indexes to indexes supported in the future by creating RANGE indexes in parallel.
	 * The new RANGE indexes will have the same name with the default suffix appended.
	 *
	 * @return The refactoring ready to use
	 */
	static MigrateBTreeIndexes createFutureIndexes() {
		return createFutureIndexes(null);
	}

	/**
	 * Prepares migration of old B-tree indexes to indexes supported in the future by creating RANGE indexes in parallel.
	 * The new RANGE indexes will have the same name with the configured suffix appended.
	 *
	 * @param suffix The suffix to use
	 * @return The refactoring ready to use
	 */
	static MigrateBTreeIndexes createFutureIndexes(String suffix) {
		return new DefaultMigrateBTreeIndexes(false, suffix, Collections.emptyMap(), Collections.emptyList());
	}

	/**
	 * Replaces all B-tree indexes and constraints backed with B-tree indexes with new Range indexes.
	 *
	 * @return The refactoring ready to use
	 */
	static MigrateBTreeIndexes replaceBTreeIndexes() {
		return new DefaultMigrateBTreeIndexes(true, null, Collections.emptyMap(), Collections.emptyList());
	}

	/**
	 * Creates a new refactoring that uses the given type mapping. An index that appears in the given list and is not an
	 * index backing a constraint will use the configured type instead.
	 *
	 * @param typeMapping A map from name to type
	 * @return The refactoring ready to use
	 */
	MigrateBTreeIndexes withTypeMapping(Map<String, Index.Type> typeMapping);

	/**
	 * Creates a new refactoring that ignores the set of configured items during migration.
	 *
	 * @param ignoredItems Indexes and constraints to ignore
	 * @return The refactoring ready to use
	 */
	MigrateBTreeIndexes withIgnoredItems(Collection<String> ignoredItems);
}
