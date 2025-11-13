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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * Discovers all Java-based migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.3
 */
final class JavaBasedMigrationDiscoverer implements Discoverer<JavaBasedMigration> {

	@Override
	public Collection<JavaBasedMigration> discover(MigrationContext context) {

		MigrationsConfig config = context.getConfig();
		if (config.getPackagesToScan().length == 0) {
			return Collections.emptyList();
		}

		try (ScanResult scanResult = new ClassGraph().enableAllInfo()
			.acceptPackages(config.getPackagesToScan())
			.enableExternalClasses()
			.scan()) {

			return scanResult.getClassesImplementing(JavaBasedMigration.class.getName())
				.loadClasses(JavaBasedMigration.class)
				.stream()
				.filter(c -> !Modifier.isAbstract(c.getModifiers()))
				.map(c -> {
					try {
						return JavaBasedMigration.getDefaultConstructorFor(c).newInstance();
					}
					catch (Exception ex) {
						throw new MigrationsException("Could not instantiate migration " + c.getName(), ex);
					}
				})
				.toList();
		}
	}

}
