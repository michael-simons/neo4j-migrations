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
package ac.simons.neo4j.migrations.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
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

		var classLoader = Objects.requireNonNullElseGet(Thread.currentThread().getContextClassLoader(),
				JavaBasedMigrationDiscoverer.class::getClassLoader);

		try (ScanResult scanResult = new ClassGraph().enableClasspath()
			.enableNonSystemModules()
			.enableClassInfo()
			.ignoreClassVisibility()
			.acceptPackages(config.getPackagesToScan())
			.enableExternalClasses()
			.scan()) {

			return scanResult.getAllClassesImplementing(JavaBasedMigration.class)
				.getStandardClasses()
				.filter(ci -> !ci.isAbstract())
				.stream()
				.map(ci -> instantiate(ci, classLoader))
				.toList();
		}
	}

	private static JavaBasedMigration instantiate(ClassInfo classInfo, ClassLoader classLoader) {
		try {
			var type = Class.forName(classInfo.getName(), false, classLoader).asSubclass(JavaBasedMigration.class);
			return JavaBasedMigration.getDefaultConstructorFor(type).newInstance();
		}
		catch (Exception ex) {
			throw new MigrationsException("Could not instantiate migration " + classInfo.getName(), ex);
		}
	}

}
