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
package ac.simons.neo4j.migrations.quarkus.deployment;

import ac.simons.neo4j.migrations.quarkus.runtime.StaticClasspathResourceScanner;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains a static classpath resource scanner.
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
final class ClasspathResourceScannerBuildItem extends SimpleBuildItem {

	private final StaticClasspathResourceScanner resourceScanner;

	ClasspathResourceScannerBuildItem(StaticClasspathResourceScanner resourceScanner) {
		this.resourceScanner = resourceScanner;
	}

	/**
	 * {@return the actual scanner (created at build-time)}
	 */
	StaticClasspathResourceScanner getScanner() {
		return this.resourceScanner;
	}

}
