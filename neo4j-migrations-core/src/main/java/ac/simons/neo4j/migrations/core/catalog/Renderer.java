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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders constraints. This indirection exists to decouple the definition of a constraint
 * from the underlying database.
 *
 * @param <T> the type of things this renderer supports
 * @author Michael J. Simons
 * @since 1.7.0
 */
public interface Renderer<T> {

	/**
	 * Retrieves a renderer for the given {@link Format} and given {@code item}.
	 * @param format the format in which to render
	 * @param type the item that should be rendered
	 * @param <T> the type of the item
	 * @return a renderer for the given type
	 * @throws UnsupportedOperationException in case the combination of format and type is
	 * not supported
	 */
	@SuppressWarnings("unchecked")
	static <T> Renderer<T> get(Format format, T type) {
		return Renderer.get(format, (Class<T>) type.getClass());
	}

	/**
	 * Retrieves a renderer for the given {@link Format} and the type matching
	 * {@literal type}.
	 * @param format the format in which to render
	 * @param type the type that should be rendered
	 * @param <T> the types specific type
	 * @return a renderer for the given type
	 * @throws UnsupportedOperationException in case the combination of format and type is
	 * not supported
	 */
	@SuppressWarnings("unchecked")
	static <T> Renderer<T> get(Format format, Class<T> type) {

		if (format == Format.CYPHER) {
			if (Constraint.class.isAssignableFrom(type)) {
				return (Renderer<T>) ConstraintToCypherRenderer.INSTANCE;
			}
			else if (Index.class.isAssignableFrom(type)) {
				return (Renderer<T>) IndexToCypherRenderer.INSTANCE;
			}
			else if (Catalog.class.isAssignableFrom(type)) {
				return (Renderer<T>) CatalogToCypherRenderer.INSTANCE;
			}
		}
		else if (format == Format.XML) {
			if (Constraint.class.isAssignableFrom(type)) {
				return (Renderer<T>) ConstraintToXMLRenderer.INSTANCE;
			}
			else if (Index.class.isAssignableFrom(type)) {
				return (Renderer<T>) IndexToXMLRenderer.INSTANCE;
			}
			else if (Catalog.class.isAssignableFrom(type)) {
				return (Renderer<T>) CatalogToXMLRenderer.INSTANCE;
			}
		}

		throw new UnsupportedOperationException(
				"Unsupported combination of format (" + format + ") and type (" + type + ").");
	}

	/**
	 * Renders a schema item.
	 * @param item the item to be rendered
	 * @param config the context in which the constraint is to be rendered.
	 * @param target the target to write to. Will not be closed.
	 * @throws IllegalStateException in case a given item cannot be rendered with the
	 * features requested in the given context.
	 * @throws IOException if rendering fails due to issues with the {@literal target}
	 */
	void render(T item, RenderConfig config, OutputStream target) throws IOException;

	/**
	 * Renders a schema item.
	 * @param item the item to be rendered
	 * @param config the configuration to render
	 * @return the textual representation of the item in the given context, ready to be
	 * executed.
	 * @throws UncheckedIOException any {@link IOException} from
	 * {@link #render(T, RenderConfig, OutputStream)} is rethrown here
	 */
	default String render(T item, RenderConfig config) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

			render(item, config, bout);
			bout.flush();

			return bout.toString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * The target formats.
	 */
	enum Format {

		/**
		 * The cypher dialect is defined by the renders context.
		 */
		CYPHER,
		/**
		 * Vendor neutral XML, matching the types defined in {@literal migration.xsd}.
		 */
		XML

	}

}
