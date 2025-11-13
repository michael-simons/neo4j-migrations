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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.Discoverer;
import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.MigrationsException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import org.springframework.beans.factory.ObjectProvider;

/**
 * A discoverer for {@link JavaBasedMigration Java based migrations} using the Spring
 * context, thus enable components being used as migrations.
 *
 * @author Michael J. Simons
 * @since 2.11.0
 */
final class ApplicationContextAwareDiscoverer implements Discoverer<JavaBasedMigration> {

	private final ObjectProvider<JavaBasedMigration> javaBasedMigrations;

	ApplicationContextAwareDiscoverer(ObjectProvider<JavaBasedMigration> javaBasedMigrations) {
		this.javaBasedMigrations = javaBasedMigrations;
	}

	@Override
	public Collection<JavaBasedMigration> discover(MigrationContext context) {

		var config = context.getConfig();
		var result = new ArrayList<JavaBasedMigration>();

		// First use all beans from the context
		this.javaBasedMigrations.forEach(result::add);
		// And keep a set of the already discovered types
		var loadedClasses = result.stream().map(JavaBasedMigration::getClass).collect(Collectors.toSet());

		// If configured, use the same algorithm as the default scanner
		if (config.getPackagesToScan().length != 0) {
			try (ScanResult scanResult = new ClassGraph().enableAllInfo()
				.acceptPackages(config.getPackagesToScan())
				.enableExternalClasses()
				.scan()) {

				scanResult.getClassesImplementing(JavaBasedMigration.class.getName())
					.loadClasses(JavaBasedMigration.class)
					.stream()
					.filter(c -> !(Modifier.isAbstract(c.getModifiers()) || loadedClasses.contains(c)))
					.map(c -> {
						try {
							return JavaBasedMigration.getDefaultConstructorFor(c).newInstance();
						}
						catch (Exception ex) {
							throw new MigrationsException("Could not instantiate migration " + c.getName(), ex);
						}
					})
					.forEach(result::add);

			}
		}
		return List.copyOf(result);
	}

}
