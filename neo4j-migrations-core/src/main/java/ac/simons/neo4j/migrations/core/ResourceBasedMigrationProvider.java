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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * An interface defining a provider with a set of supported extension and a mapper from resource to {@link Migration}.
 * The interface is ordered so that we can pick the accordingly when one provider maps to the same extension.
 *
 * @author Michael J. Simons
 * @soundtrack Fatoni - Andorra
 * @since TBA
 */
public interface ResourceBasedMigrationProvider extends Ordered {

	/**
	 * @return a collection containing unique providers (only one for each extension)
	 */
	static Collection<ResourceBasedMigrationProvider> unique() {

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

	/**
	 * @return the file extension this resource based migration provider deals with
	 */
	String getExtension();

	/**
	 * Handles the resource of the given context and produces a new migration.
	 *
	 * @param ctx The context containing the resource and other information
	 * @return The migration based on the resource.
	 */
	Migration handle(ResourceContext ctx);
}
