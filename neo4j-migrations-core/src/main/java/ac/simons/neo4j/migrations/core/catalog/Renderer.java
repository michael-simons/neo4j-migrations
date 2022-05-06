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
package ac.simons.neo4j.migrations.core.catalog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders constraints. This indirection exists to decouple the definition of a constraint from the underlying database.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
interface Renderer<T extends CatalogItem<?>> {

	/**
	 * Renders a catalog item.
	 *
	 * @param item    The item to be rendered
	 * @param context The context in which the constraint is to be rendered.
	 * @param target The target to write to. Will not be closed.
	 */
	void render(T item, RenderContext context, OutputStream target) throws IOException;

	/**
	 * Renders a catalog item.
	 *
	 * @param item    The item to be rendered
	 * @param context The context in which the constraint is to be rendered.
	 * @return The textual representation of the item in the given context, ready to be executed.
	 */
	default String render(T item, RenderContext context) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

			render(item, context, bout);
			bout.flush();

			return new String(bout.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
