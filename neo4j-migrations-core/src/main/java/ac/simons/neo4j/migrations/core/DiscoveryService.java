/*
 * Copyright 2020-2021 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates {@link Discoverer discoverers}.
 *
 * @author Michael J. Simons
 * @since 0.0.3
 */
final class DiscoveryService {

	private final List<Discoverer<Migration>> migrationDiscoverers;

	private final List<Discoverer<Callback>> callbackDiscoverers;

	DiscoveryService() {
		this.migrationDiscoverers = Collections.unmodifiableList(
			Arrays.asList(new JavaBasedMigrationDiscoverer(),
				CypherResourceDiscoverer.forMigrations()));

		this.callbackDiscoverers = Collections.singletonList(CypherResourceDiscoverer.forCallbacks());
	}

	/**
	 * @return An unmodifiable list of migrations sorted by version.
	 */
	List<Migration> findMigrations(MigrationContext context) {

		List<Migration> migrations = new ArrayList<>();
		try {
			for (Discoverer<Migration> discoverer : this.migrationDiscoverers) {
				migrations.addAll(discoverer.discover(context));
			}
		} catch (Exception e) {
			throw new MigrationsException("Unexpected error while scanning for migrations", e);
		}

		Collections
			.sort(migrations, Comparator.comparing(Migration::getVersion, new MigrationVersion.VersionComparator()));
		return Collections.unmodifiableList(migrations);
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
