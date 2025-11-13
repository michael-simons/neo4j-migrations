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

/**
 * Anonymize a catalog item.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class AnonymousCatalogItem implements Formattable {

	private final AbstractCatalogItem<?> delegate;

	AnonymousCatalogItem(AbstractCatalogItem<?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {
		try {
			formatter.out().append(this.delegate.getClass().getSimpleName().toUpperCase(Locale.ROOT));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
