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

import java.net.URL;
import java.util.List;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;

/**
 * A {@link ClasspathResourceScanner} that scans the class path for cypher resources on
 * demand.
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
final class DefaultClasspathResourceScanner implements ClasspathResourceScanner {

	@Override
	public List<URL> scan(List<String> locations) {

		String[] paths = locations.toArray(new String[0]);
		try (ScanResult scanResult = new ClassGraph().acceptPaths(paths).scan();
				ResourceList allResources = scanResult.getAllResources().nonClassFilesOnly()) {
			return allResources.stream().map(Resource::getURL).toList();
		}
	}

}
