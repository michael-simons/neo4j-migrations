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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Discovers all Java-based migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.3
 */
final class JavaBasedMigrationDiscoverer implements Discoverer<Migration> {

	@Override
	public Collection<Migration> discover(MigrationContext context) {

		MigrationsConfig config = context.getConfig();
		if (config.getPackagesToScan().length == 0) {
			return Collections.emptyList();
		}

		try (ScanResult scanResult = new ClassGraph()
				.enableAllInfo()
				.acceptPackages(config.getPackagesToScan())
				.enableExternalClasses()
				.scan()) {

			return scanResult
					.getClassesImplementing(JavaBasedMigration.class.getName()).loadClasses(Migration.class)
					.stream()
					.map(c -> {
						try {
							return getConstructor(c).newInstance();
						} catch (Exception e) {
							throw new MigrationsException("Could not instantiate migration " + c.getName(), e);
						}
					}).collect(Collectors.toList());
		}
	}

	@SuppressWarnings("squid:S3011") // Very much the point of the whole thing
	private static Constructor<Migration> getConstructor(Class<Migration> c) throws NoSuchMethodException {
		Constructor<Migration> ctr = c.getDeclaredConstructor();
		ctr.setAccessible(true);
		return ctr;
	}
}
