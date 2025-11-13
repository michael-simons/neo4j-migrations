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
import java.io.UncheckedIOException;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;

import ac.simons.neo4j.migrations.core.Neo4jVersion;

/**
 * A helper class keeping the rendering code stable while getting the {@link Formattable}
 * trait out of {@link CatalogItem catalog items} in 2.0 and higher.
 *
 * @author Michael J. Simons
 * @since 2.0.0
 */
final class FormattableCatalogItem implements Formattable {

	private final CatalogItem<?> delegate;

	private final Neo4jVersion version;

	FormattableCatalogItem(CatalogItem<?> delegate, Neo4jVersion version) {
		this.delegate = delegate;
		this.version = version;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {

		Appendable out = formatter.out();
		try {
			out.append(this.delegate.getClass().getSimpleName().toUpperCase(Locale.ROOT));
			var name = this.delegate.getName();
			if (!(name instanceof GeneratedName)) {
				out.append(' ').append(this.version.sanitizeSchemaName(name.getValue()));
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
