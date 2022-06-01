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
package ac.simons.neo4j.migrations.core.internal;

import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Utilities around services
 *
 * @author Michael J. Simons
 * @soundtrack Fatoni - Andorra
 * @since TBA
 */
public final class Services {

	/**
	 * @return a collection containing unique providers (only one for each extension)
	 */
	public static Collection<ResourceBasedMigrationProvider> loadUniqueResourceBasedMigrationProviders() {

		Map<String, ResourceBasedMigrationProvider> providers = new HashMap<>();
		ServiceLoader<ResourceBasedMigrationProvider> loader = ServiceLoader.load(ResourceBasedMigrationProvider.class);
		for (ResourceBasedMigrationProvider provider : loader) {
			String extension = provider.getExtension();
			if (providers.containsKey(extension) && providers.get(extension).getOrder() < provider.getOrder()) {
				continue;
			}
			providers.put(extension, provider);
		}

		return Collections.unmodifiableCollection(providers.values());
	}

	private Services() {
	}
}
