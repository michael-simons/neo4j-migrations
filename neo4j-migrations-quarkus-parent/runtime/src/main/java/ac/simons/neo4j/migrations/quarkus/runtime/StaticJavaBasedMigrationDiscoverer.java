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
import ac.simons.neo4j.migrations.core.internal.Reflections;
import io.github.classgraph.ClassGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Anarchie und Alltag
 * @since 1.3.0
 */
public class StaticJavaBasedMigrationDiscoverer implements Discoverer<JavaBasedMigration> {

	/**
	 * Creates a new discoverer from a list of packages to scan.
	 *
	 * @param packagesToScan The list of packages to scan
	 * @return a correctly initialized discoverer
	 */
	public static StaticJavaBasedMigrationDiscoverer from(List<String> packagesToScan) {

		try (var scanResult = new ClassGraph()
			.enableAllInfo()
			.acceptPackages(packagesToScan.toArray(new String[0]))
			.enableExternalClasses()
			.scan()) {

			var classes = scanResult
				.getClassesImplementing(JavaBasedMigration.class.getName()).loadClasses(JavaBasedMigration.class)
				.stream()
				.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
			var discoverer = new StaticJavaBasedMigrationDiscoverer();
			discoverer.setMigrationClasses(classes);
			return discoverer;
		}
	}

	private Set<Class<JavaBasedMigration>> migrationClasses = Set.of();

	/**
	 * @return the list of discovered classes.
	 */
	public Set<Class<JavaBasedMigration>> getMigrationClasses() {
		return migrationClasses;
	}

	/**
	 * This method may not be used outside Quarkus internal code.
	 *
	 * @param migrationClasses a new list of discovered classes
	 */
	public void setMigrationClasses(Set<Class<JavaBasedMigration>> migrationClasses) {
		this.migrationClasses = migrationClasses;
	}

	@Override
	public Collection<JavaBasedMigration> discover(MigrationContext context) {

		var packagesToScan = new HashSet<>(Arrays.asList(context.getConfig().getPackagesToScan()));
		return this.migrationClasses.stream()
			.filter(c -> packagesToScan.contains(c.getPackageName()))
			.map(c -> {
				try {
					return Reflections.getDefaultConstructorFor(c).newInstance();
				} catch (Exception e) {
					throw new MigrationsException("Could not instantiate migration " + c.getName(), e);
				}
			}).collect(Collectors.toList());
	}
}
