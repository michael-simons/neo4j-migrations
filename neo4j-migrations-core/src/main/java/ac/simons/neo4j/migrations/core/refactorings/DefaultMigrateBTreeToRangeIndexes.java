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

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Result;

/**
 * @author Michael J. Simons
 * @soundtrack Kraftklub - KARGO
 */
final class DefaultMigrateBTreeToRangeIndexes implements MigrateBTreeToRangeIndexes {

	private final QueryRunner.FeatureSet featureSet;

	/**
	 * Set to {@literal true} to drop old {@literal btree} indexes and just create new {@literal range} backed indexes
	 * and constraints. The default value is {@literal false}, which will make this refactoring create new indexes and
	 * constraints in parallel to the old ones with a derived name. They won't do anything on a 4.4 instance but will
	 * avoid downtime when migrating to 5.0.
	 */
	private final boolean dropOldIndexes;

	DefaultMigrateBTreeToRangeIndexes(boolean dropOldIndexes) {

		this.dropOldIndexes = dropOldIndexes;
		this.featureSet = QueryRunner.defaultFeatureSet()
			.withRequiredVersion("4.4");
	}

	/**
	 * Returns a map of constraints backed by indexes based on btrees. The map key is the id of the owning index for
	 * later processing.
	 *
	 * @param queryRunner Might be a session or tx
	 * @param owners      An optional list of owning indexes. If {@literal null}, this method will retrieve the indexes itself
	 * @return A map of constraints backed by indexes based on btrees
	 */
	Map<Long, Constraint> findBTreeBasedConstraints(QueryRunner queryRunner, Map<Long, Index> owners) {
		Map<Long, Index> indexLookup = owners == null ? findBTreeBasedIndexes(queryRunner) : owners;
		return queryRunner.run(new Query("SHOW constraints YIELD * WHERE ownedIndexId IN $owners",
				Collections.singletonMap("owners", indexLookup.keySet())))
			.stream().collect(Collectors.toMap(r -> r.get("ownedIndexId").asLong(), Constraint::parse));
	}

	/**
	 * Returns a map of indexes backed by btrees. The map key is the id of the index.
	 *
	 * @param queryRunner Might be a session or tx
	 * @return A map of indexes
	 */
	Map<Long, Index> findBTreeBasedIndexes(QueryRunner queryRunner) {
		Result result = queryRunner.run(new Query("SHOW indexes YIELD * WHERE toUpper(type) = 'BTREE' RETURN *"));
		return result.stream()
			.collect(Collectors.toMap(r -> r.get("id").asLong(), Index::parse));
	}

	/**
	 * Creates a list of catalog items that can be replaced by new range indexes
	 *
	 * @param queryRunner Might be a session or tx
	 * @return A list with items that need processing
	 */
	List<CatalogItem<?>> findBTreeBasedItems(QueryRunner queryRunner) {

		Map<Long, Index> indexes = findBTreeBasedIndexes(queryRunner);
		Map<Long, Constraint> constraints = findBTreeBasedConstraints(queryRunner, indexes);

		List<CatalogItem<?>> result = new ArrayList<>(constraints.values());
		indexes
			.entrySet()
			.stream()
			.filter(e -> !constraints.containsKey(e.getKey()))
			.map(Map.Entry::getValue)
			.forEach(result::add);
		return result;
	}

	@Override
	public Counters apply(RefactoringContext refactoringContext) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
