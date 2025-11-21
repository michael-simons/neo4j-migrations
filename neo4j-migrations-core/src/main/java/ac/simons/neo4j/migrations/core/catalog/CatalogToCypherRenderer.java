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
package ac.simons.neo4j.migrations.core.catalog;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a catalog into Cypher.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
@SuppressWarnings("squid:S6548")
enum CatalogToCypherRenderer implements Renderer<Catalog> {

	INSTANCE;

	@Override
	public void render(Catalog catalog, RenderConfig config, OutputStream target) throws IOException {

		byte[] separator = (";" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
		Map<Class<CatalogItem<?>>, Renderer<CatalogItem<?>>> cachedRenderer = new ConcurrentHashMap<>(4);
		for (CatalogItem<?> item : catalog.getItems()) {
			@SuppressWarnings("unchecked")
			Renderer<CatalogItem<?>> renderer = cachedRenderer.computeIfAbsent((Class<CatalogItem<?>>) item.getClass(),
					type -> Renderer.get(Format.CYPHER, type));
			renderer.render(item, config, target);
			target.write(separator);
		}
		target.flush();
	}

}
