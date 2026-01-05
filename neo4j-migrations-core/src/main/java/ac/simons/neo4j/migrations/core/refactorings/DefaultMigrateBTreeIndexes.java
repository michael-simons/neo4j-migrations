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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;

/**
 * Implements the {@link MigrateBTreeIndexes migration from btree index} refactoring.
 *
 * @author Michael J. Simons
 */
final class DefaultMigrateBTreeIndexes implements MigrateBTreeIndexes {

	private static final String OPTION_USE_RANGE_INDEX_PROVIDER = "{`indexProvider`: \"range-1.0\"}";

	private final QueryRunner.FeatureSet featureSet;

	/**
	 * Set to {@literal true} to drop old {@literal btree} indexes and just create new
	 * {@literal range} backed indexes and constraints. The default value is
	 * {@literal false}, which will make this refactoring create new indexes and
	 * constraints in parallel to the old ones with a derived name. They won't do anything
	 * on a 4.4 instance but will avoid downtime when migrating to 5.0.
	 */
	private final boolean dropOldIndexes;

	/**
	 * The suffix appended to the replacement constraints and indexes in case
	 * {@link #dropOldIndexes} is {@literal false}. Defaults to {@link #DEFAULT_SUFFIX}.
	 */
	private final String suffix;

	/**
	 * A map with optional type mappings: Any named index appearing in this map will be
	 * created with the given type and not default to {@code RANGE}.
	 */
	private final Map<String, Index.Type> typeMapping;

	/**
	 * A set of catalog items (both indexes and constraints) that are ignored and not
	 * touched.
	 */
	private final Set<String> excludes;

	/**
	 * A set of catalog items (both indexes and constraints) that are included.
	 */
	private final Set<String> includes;

	private final RenderConfig createConfigConstraint;

	private final RenderConfig createConfigIndex;

	private final RenderConfig dropConfig;

	DefaultMigrateBTreeIndexes() {
		this(false, null, null, null, null);
	}

	DefaultMigrateBTreeIndexes(boolean dropOldIndexes, String suffix, Map<String, Index.Type> typeMapping,
			Collection<String> excludes, Collection<String> includes) {

		this.dropOldIndexes = dropOldIndexes;
		this.suffix = ((suffix != null) && !suffix.trim().isEmpty()) ? suffix.trim() : DEFAULT_SUFFIX;
		this.typeMapping = (typeMapping != null) ? new HashMap<>(typeMapping) : Collections.emptyMap();
		this.excludes = (excludes != null) ? new HashSet<>(excludes) : Set.of();
		this.includes = (includes != null) ? new HashSet<>(includes) : Set.of();

		this.featureSet = QueryRunner.defaultFeatureSet().withRequiredVersion("4.4");
		this.createConfigConstraint = RenderConfig.create()
			.idempotent(true)
			.forVersionAndEdition(this.featureSet.requiredVersion(), "ENTERPRISE")
			.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
				@Override
				public boolean includingOptions() {
					return true;
				}

				@Override
				public boolean useExplicitPropertyIndexType() {
					return true;
				}
			}));
		this.createConfigIndex = this.createConfigConstraint
			.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
				@Override
				public boolean useExplicitPropertyIndexType() {
					return true;
				}
			}));
		this.dropConfig = RenderConfig.drop()
			.ifExists()
			.forVersionAndEdition(this.featureSet.requiredVersion(), "ENTERPRISE");
	}

	/**
	 * Returns a map of constraints backed by indexes based on btrees. The map key is the
	 * id of the owning index for later processing.
	 * @param queryRunner might be a session or tx
	 * @param owners an optional list of owning indexes. If {@literal null}, this method
	 * will retrieve the indexes itself
	 * @return a map of constraints backed by indexes based on btrees
	 */
	Map<Long, Constraint> findBTreeBasedConstraints(QueryRunner queryRunner, Map<Long, Index> owners) {
		Map<Long, Index> indexLookup = (owners != null) ? owners : findBTreeBasedIndexes(queryRunner);
		return queryRunner
			.run(new Query("SHOW constraints YIELD * WHERE ownedIndexId IN $owners",
					Collections.singletonMap("owners", indexLookup.keySet())))
			.stream()
			.collect(Collectors.toMap(r -> r.get("ownedIndexId").asLong(), Constraint::parse));
	}

	/**
	 * Returns a map of indexes backed by btrees. The map key is the id of the index.
	 * @param queryRunner might be a session or tx
	 * @return a map of indexes
	 */
	Map<Long, Index> findBTreeBasedIndexes(QueryRunner queryRunner) {
		Result result = queryRunner.run(new Query("SHOW indexes YIELD * WHERE toUpper(type) = 'BTREE' RETURN *"));
		return result.stream().collect(Collectors.toMap(r -> r.get("id").asLong(), Index::parse));
	}

	/**
	 * Creates a list of catalog items that can be replaced by new range indexes.
	 * @param queryRunner might be a session or tx
	 * @return a list with items that need processing
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	List<CatalogItem<?>> findBTreeBasedItems(QueryRunner queryRunner) {

		var versions = queryRunner
			.run(new Query("CALL dbms.components() YIELD name, versions WHERE name = 'Neo4j Kernel' RETURN versions"))
			.single()
			.get("versions")
			.asList(Value::asString);
		if (versions.stream().anyMatch(v -> v.startsWith("5."))) {
			return List.of();
		}

		Map<Long, Index> indexes = findBTreeBasedIndexes(queryRunner);
		Map<Long, Constraint> constraints = findBTreeBasedConstraints(queryRunner, indexes);

		List<CatalogItem<?>> result = new ArrayList<>();
		Predicate<CatalogItem<?>> notExcluded = c -> !this.excludes.contains(c.getName().getValue());
		notExcluded = notExcluded.and(c -> this.includes.isEmpty() || this.includes.contains(c.getName().getValue()));
		constraints.values().stream().filter(notExcluded).forEach(result::add);
		indexes.entrySet()
			.stream()
			.filter(e -> !constraints.containsKey(e.getKey()))
			.map(Map.Entry::getValue)
			.filter(notExcluded)
			.forEach(result::add);
		return result;
	}

	/**
	 * Migrates an existing item to a new form (adding options and potentially changes the
	 * name).
	 * @param old the old catalog item
	 * @return modified one
	 * @throws IllegalArgumentException on {@literal null} or unsupported items
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	CatalogItem<?> migrate(CatalogItem<?> old) {

		if (old == null) {
			throw new IllegalArgumentException("Cannot migrate null items");
		}

		String oldName = old.getName().getValue();
		if (old instanceof Constraint constraint) {
			return constraint.withOptions(OPTION_USE_RANGE_INDEX_PROVIDER)
				.withName(this.dropOldIndexes ? oldName : oldName + this.suffix);
		}
		else if (old instanceof Index index) {
			return index.withOptions(OPTION_USE_RANGE_INDEX_PROVIDER)
				.withName(this.dropOldIndexes ? oldName : oldName + this.suffix)
				.withType(this.typeMapping.getOrDefault(oldName, Index.Type.PROPERTY));
		}

		throw new IllegalArgumentException("Cannot migrate catalog items of type " + old.getClass());
	}

	@Override
	public Counters apply(RefactoringContext context) {

		Function<CatalogItem<?>, Renderer<CatalogItem<?>>> rendererProvider = new Function<>() {
			final Map<Class<CatalogItem<?>>, Renderer<CatalogItem<?>>> cachedRenderer = new ConcurrentHashMap<>(4);

			@SuppressWarnings("unchecked")
			@Override
			public Renderer<CatalogItem<?>> apply(CatalogItem<?> item) {
				return this.cachedRenderer.computeIfAbsent((Class<CatalogItem<?>>) item.getClass(),
						type -> Renderer.get(Renderer.Format.CYPHER, type));
			}
		};

		Counters counters = Counters.empty();
		try (QueryRunner tx = context.getQueryRunner(this.featureSet)) {
			List<CatalogItem<?>> bTreeBasedItems = findBTreeBasedItems(tx);
			StringJoiner dropStatements = new StringJoiner(System.lineSeparator(), System.lineSeparator(), "");
			for (CatalogItem<?> item : bTreeBasedItems) {
				Renderer<CatalogItem<?>> renderer = rendererProvider.apply(item);

				String dropStatement = renderer.render(item, this.dropConfig);
				if (this.dropOldIndexes) {
					counters = counters.add(new DefaultCounters(tx.run(new Query(dropStatement)).consume().counters()));
				}
				else {
					dropStatements.add(dropStatement + ";");
				}

				CatalogItem<?> migratedItem = migrate(item);
				RenderConfig config = (item instanceof Index) ? this.createConfigIndex : this.createConfigConstraint;
				counters = counters.add(new DefaultCounters(
						tx.run(new Query(renderer.render(migratedItem, config))).consume().counters()));
			}

			if (!this.dropOldIndexes && LOGGER.isLoggable(Level.INFO)
					&& (counters.indexesAdded() + counters.constraintsAdded()) > 0) {
				LOGGER.log(Level.INFO,
						"Future indexes have been created. Use the following statements to drop all BTREE based constraints and indexes:{0}",
						dropStatements);
			}
		}

		return counters;
	}

	@Override
	public MigrateBTreeIndexes withTypeMapping(Map<String, Index.Type> newTypeMapping) {
		if (Objects.equals(this.typeMapping, newTypeMapping)
				|| (newTypeMapping == null && this.typeMapping.isEmpty())) {
			return this;
		}

		return new DefaultMigrateBTreeIndexes(this.dropOldIndexes, this.suffix, newTypeMapping, this.excludes,
				this.includes);
	}

	@Override
	public MigrateBTreeIndexes withExcludes(Collection<String> newExcludes) {
		if (Objects.equals(this.excludes, newExcludes)
				|| ((newExcludes == null || newExcludes.isEmpty()) && this.excludes.isEmpty())) {
			return this;
		}

		return new DefaultMigrateBTreeIndexes(this.dropOldIndexes, this.suffix, this.typeMapping, newExcludes,
				this.includes);
	}

	@Override
	public MigrateBTreeIndexes withIncludes(Collection<String> newIncludes) {
		if (Objects.equals(this.includes, newIncludes)
				|| ((newIncludes == null || newIncludes.isEmpty()) && this.includes.isEmpty())) {
			return this;
		}

		return new DefaultMigrateBTreeIndexes(this.dropOldIndexes, this.suffix, this.typeMapping, this.excludes,
				newIncludes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultMigrateBTreeIndexes that = (DefaultMigrateBTreeIndexes) o;
		return this.dropOldIndexes == that.dropOldIndexes && this.suffix.equals(that.suffix)
				&& this.typeMapping.equals(that.typeMapping) && this.excludes.equals(that.excludes)
				&& this.includes.equals(that.includes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.dropOldIndexes, this.suffix, this.typeMapping, this.excludes);
	}

}
