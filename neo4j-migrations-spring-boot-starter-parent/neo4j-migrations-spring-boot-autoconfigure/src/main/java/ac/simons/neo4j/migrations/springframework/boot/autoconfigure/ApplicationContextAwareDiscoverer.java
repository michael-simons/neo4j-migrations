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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.Discoverer;
import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.MigrationsException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
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

	private final ClassLoader classLoader;

	ApplicationContextAwareDiscoverer(ObjectProvider<JavaBasedMigration> javaBasedMigrations, ClassLoader classLoader) {
		this.javaBasedMigrations = javaBasedMigrations;
		this.classLoader = classLoader;
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
			try (ScanResult scanResult = new ClassGraph().enableClasspath()
				.enableNonSystemModules()
				.enableClassInfo()
				.ignoreClassVisibility()
				.acceptPackages(config.getPackagesToScan())
				.enableExternalClasses()
				.scan()) {

				scanResult.getAllClassesImplementing(JavaBasedMigration.class)
					.getStandardClasses()
					.filter(ci -> !ci.isAbstract())
					.stream()
					.map(this::loadClass)
					.filter(c -> !loadedClasses.contains(c))
					.map(ApplicationContextAwareDiscoverer::instantiate)
					.forEach(result::add);
			}
		}
		return List.copyOf(result);
	}

	private Class<? extends JavaBasedMigration> loadClass(ClassInfo classInfo) {
		try {
			return Class.forName(classInfo.getName(), false, this.classLoader).asSubclass(JavaBasedMigration.class);
		}
		catch (Exception ex) {
			throw new MigrationsException("Could not load migration " + classInfo.getName(), ex);
		}
	}

	private static JavaBasedMigration instantiate(Class<? extends JavaBasedMigration> type) {
		try {
			return JavaBasedMigration.getDefaultConstructorFor(type).newInstance();
		}
		catch (Exception ex) {
			throw new MigrationsException("Could not instantiate migration " + type.getName(), ex);
		}
	}

}
