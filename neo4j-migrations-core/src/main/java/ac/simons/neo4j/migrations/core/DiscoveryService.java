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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.catalog.Catalog;

/**
 * Orchestrates {@link Discoverer discoverers}.
 *
 * @author Michael J. Simons
 * @since 0.0.3
 */
final class DiscoveryService {

	private final List<Discoverer<? extends Migration>> migrationDiscoverers;

	private final List<Discoverer<Callback>> callbackDiscoverers;

	DiscoveryService() {
		this(new JavaBasedMigrationDiscoverer(), new DefaultClasspathResourceScanner());
	}

	DiscoveryService(Discoverer<? extends Migration> migrationClassesDiscoverer,
			ClasspathResourceScanner resourceScanner) {

		this.migrationDiscoverers = List.of(migrationClassesDiscoverer,
				ResourceDiscoverer.forMigrations(resourceScanner));
		this.callbackDiscoverers = List.of(ResourceDiscoverer.forCallbacks(resourceScanner));
	}

	/**
	 * Finds all migrations in this context.
	 * @param context the context in which the migrations run
	 * @return an unmodifiable list of migrations sorted by version
	 */
	List<Migration> findMigrations(MigrationContext context) {

		List<Migration> migrations = new ArrayList<>();
		try {
			for (Discoverer<? extends Migration> discoverer : this.migrationDiscoverers) {
				migrations.addAll(discoverer.discover(context));
			}
		}
		catch (Exception ex) {
			throw new MigrationsException("Unexpected error while scanning for migrations", ex);
		}

		List<MigrationWithPreconditions> cypherBasedMigrations = migrations.stream()
			.filter(MigrationWithPreconditions.class::isInstance)
			.map(MigrationWithPreconditions.class::cast)
			.toList();
		Map<Migration, List<Precondition>> migrationsAndPreconditions = new HashMap<>();
		computeAlternativeChecksums(cypherBasedMigrations, migrationsAndPreconditions);

		migrations.removeIf(migration -> hasUnmetPreconditions(migrationsAndPreconditions, migration, context));
		migrations.sort(Comparator.comparing(Migration::getVersion, context.getConfig().getVersionComparator()));
		Catalog catalog = context.getCatalog();
		if (catalog instanceof WriteableCatalog writeableSchema) {
			migrations.stream()
				.filter(CatalogBasedMigration.class::isInstance)
				.map(CatalogBasedMigration.class::cast)
				.forEach(m -> writeableSchema.addAll(m.getVersion(), m.getCatalog(), m.isResetCatalog()));
		}
		Map<MigrationVersion, List<Migration>> groupedByVersion = migrations.stream()
			.collect(Collectors.groupingBy(Migration::getVersion));
		for (Map.Entry<MigrationVersion, List<Migration>> entry : groupedByVersion.entrySet()) {
			List<Migration> v = entry.getValue();
			if (v.size() > 1) {
				MigrationVersion k = entry.getKey();
				String sources = v.stream().map(Migration::getSource).sorted().collect(Collectors.joining(", "));
				throw new MigrationsException("Duplicate version '" + k.getValue() + "' (" + sources + ")");
			}
		}
		return List.copyOf(migrations);
	}

	private void computeAlternativeChecksums(List<MigrationWithPreconditions> migrations,
			Map<Migration, List<Precondition>> migrationsAndPreconditions) {
		migrations.forEach(m -> {
			List<Precondition> preconditions = m.getPreconditions();
			migrationsAndPreconditions.put(m, preconditions);
		});

		migrations.forEach(m -> {
			if (migrationsAndPreconditions.get(m).isEmpty()) {
				return;
			}
			List<String> alternativeChecksums = migrations.stream()
				.filter(o -> o != m && o.getSource().equals(m.getSource())
						&& !migrationsAndPreconditions.get(o).isEmpty())
				.flatMap(o -> {
					Stream<String> checksum = o.getChecksum().stream();
					return Stream.concat(checksum, o.getAlternativeChecksums().stream()).distinct();
				})
				.toList();
			m.setAlternativeChecksums(alternativeChecksums);
		});
	}

	boolean hasUnmetPreconditions(Map<Migration, List<Precondition>> migrationsAndPreconditions, Migration migration,
			MigrationContext context) {

		if (!(migration instanceof MigrationWithPreconditions)) {
			return false;
		}

		Map<Precondition.Type, List<Precondition>> preconditions = migrationsAndPreconditions.get(migration)
			.stream()
			.collect(Collectors.groupingBy(Precondition::getType));

		if (preconditions.isEmpty()) {
			return false;
		}

		for (Precondition assertion : preconditions.getOrDefault(Precondition.Type.ASSERTION, List.of())) {
			if (!assertion.isMet(context)) {
				throw new MigrationsException("Could not satisfy `" + assertion + "`.");
			}
		}

		List<Precondition> unmet = preconditions.getOrDefault(Precondition.Type.ASSUMPTION, List.of())
			.stream()
			.filter(precondition -> !precondition.isMet(context))
			.toList();

		if (unmet.isEmpty()) {
			return false;
		}

		Migrations.LOGGER.log(Level.INFO, () -> String.format("Skipping %s due to unmet preconditions:%n%s",
				Migrations.toString(migration),
				unmet.stream().map(Precondition::toString).collect(Collectors.joining(System.lineSeparator()))));

		return true;
	}

	Map<LifecyclePhase, List<Callback>> findCallbacks(MigrationContext context) {

		return this.callbackDiscoverers.stream()
			.flatMap(d -> d.discover(context).stream())
			.collect(Collectors.collectingAndThen(
					Collectors.groupingBy(Callback::getPhase, Collectors.collectingAndThen(Collectors.toList(), l -> {
						l.sort(Comparator.comparing(c -> c.getOptionalDescription().orElse("")));
						return List.copyOf(l);
					})), Map::copyOf));
	}

}
