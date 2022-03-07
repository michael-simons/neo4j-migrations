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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

	DiscoveryService(Discoverer<? extends Migration> migrationClassesDiscoverer, ClasspathResourceScanner resourceScanner) {

		this.migrationDiscoverers = Collections.unmodifiableList(Arrays.asList(migrationClassesDiscoverer, CypherResourceDiscoverer.forMigrations(resourceScanner)));
		this.callbackDiscoverers = Collections.singletonList(CypherResourceDiscoverer.forCallbacks(resourceScanner));
	}

	/**
	 * @return An unmodifiable list of migrations sorted by version.
	 */
	List<Migration> findMigrations(MigrationContext context) {

		List<Migration> migrations = new ArrayList<>();
		try {
			for (Discoverer<? extends Migration> discoverer : this.migrationDiscoverers) {
				Collection<? extends Migration> discoveredMigrations = discoverer.discover(context);
				for (Migration migration : discoveredMigrations) {
					List<Precondition> unsatisfiedPreconditions = evaluatePreconditions(migration, context);
					if (unsatisfiedPreconditions.isEmpty()) {
						migrations.add(migration);
					} else {
						log(migration, unsatisfiedPreconditions);
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof MigrationsException) {
				throw e;
			}
			throw new MigrationsException("Unexpected error while scanning for migrations", e);
		}

		migrations.sort(Comparator.comparing(Migration::getVersion, new MigrationVersion.VersionComparator()));
		return Collections.unmodifiableList(migrations);
	}

	void log(Migration migration, List<Precondition> unsatisfiedPreconditions) {
		Migrations.LOGGER.log(Level.INFO, "Skipping {0} due to unsatisfied preconditions: {1}.",
			new Object[] { Migrations.toString(migration),
				unsatisfiedPreconditions.stream().map(Precondition::toString).collect(
					Collectors.joining(",")) });
	}

	List<Precondition> evaluatePreconditions(Migration migration, MigrationContext context) {

		if (!(migration instanceof CypherBasedMigration)) {
			return Collections.emptyList();
		}

		Map<Precondition.Type, List<Precondition>> preconditions = ((CypherBasedMigration) migration).getPreconditions()
			.stream().collect(Collectors.groupingBy(Precondition::getType));

		if (preconditions.isEmpty()) {
			return Collections.emptyList();
		}

		for (Precondition assertion : preconditions.getOrDefault(Precondition.Type.ASSERTION,
			Collections.emptyList())) {
			if (!assertion.isSatisfied(context)) {
				throw new MigrationsException("Could not satisfy `" + assertion + "`.");
			}
		}

		return preconditions.getOrDefault(Precondition.Type.ASSUMPTION, Collections.emptyList())
			.stream().filter(precondition -> !precondition.isSatisfied(context)).collect(Collectors.toList());
	}

	Map<LifecyclePhase, List<Callback>> findCallbacks(MigrationContext context) {

		return Collections.unmodifiableMap(this.callbackDiscoverers.stream()
			.flatMap(d -> d.discover(context).stream())
			.collect(Collectors.groupingBy(Callback::getPhase, Collectors.collectingAndThen(Collectors.toList(), l -> {
				l.sort(Comparator.comparing(c -> c.getOptionalDescription().orElse("")));
				return Collections.unmodifiableList(l);
			}))));
	}
}
