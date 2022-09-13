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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.Discoverer;
import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.MigrationsException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Anarchie und Alltag
 * @since 1.3.0
 */
public class StaticJavaBasedMigrationDiscoverer implements Discoverer<JavaBasedMigration> {

	/**
	 * Creates a new discoverer from a fixed set of already found and loaded classes.
	 *
	 * @param migrationClasses The set of classes found elsewhere
	 * @return a correctly initialized discoverer
	 */
	public static StaticJavaBasedMigrationDiscoverer of(Collection<Class<? extends JavaBasedMigration>> migrationClasses) {

		var discoverer = new StaticJavaBasedMigrationDiscoverer();
		discoverer.setMigrationClasses(Set.copyOf(migrationClasses));
		return discoverer;
	}

	private Set<Class<? extends JavaBasedMigration>> migrationClasses = Set.of();

	/**
	 * @return the list of discovered classes.
	 */
	public Set<Class<? extends JavaBasedMigration>> getMigrationClasses() {
		return migrationClasses;
	}

	/**
	 * This method may not be used outside Quarkus internal code.
	 *
	 * @param migrationClasses a new list of discovered classes
	 */
	public void setMigrationClasses(Set<Class<? extends JavaBasedMigration>> migrationClasses) {
		this.migrationClasses = migrationClasses;
	}

	@Override
	public Collection<JavaBasedMigration> discover(MigrationContext context) {

		var packagesToScan = new HashSet<>(Arrays.asList(context.getConfig().getPackagesToScan()));
		return this.migrationClasses.stream()
			.filter(c -> packagesToScan.contains(c.getPackageName()))
			.map(c -> {
				try {
					return JavaBasedMigration.getDefaultConstructorFor(c).newInstance();
				} catch (Exception e) {
					throw new MigrationsException("Could not instantiate migration " + c.getName(), e);
				}
			}).collect(Collectors.toList());
	}
}
