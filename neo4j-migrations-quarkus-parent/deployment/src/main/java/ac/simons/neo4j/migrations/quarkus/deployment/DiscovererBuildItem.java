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

import ac.simons.neo4j.migrations.quarkus.runtime.StaticJavaBasedMigrationDiscoverer;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Adds a static discoverer for Java based migration to the build items.
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
final class DiscovererBuildItem extends SimpleBuildItem {

	private final StaticJavaBasedMigrationDiscoverer discoverer;

	DiscovererBuildItem(StaticJavaBasedMigrationDiscoverer discoverer) {
		this.discoverer = discoverer;
	}

	/**
	 * {@return the actual discoverer (created at build-time)}
	 */
	StaticJavaBasedMigrationDiscoverer getDiscoverer() {
		return this.discoverer;
	}

}
