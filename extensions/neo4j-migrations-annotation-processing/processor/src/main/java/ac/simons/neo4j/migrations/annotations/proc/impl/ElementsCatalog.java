/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Value holder of catalog types.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - ...And Justice For All
 * @since 1.15.0
 */
record ElementsCatalog(
	TypeElement required,
	TypeElement unique,
	TypeElement uniqueWrapper,
	TypeElement fulltextIndex) {

	static Optional<ElementsCatalog> of(Elements elements) {
		TypeElement catalogRequired = elements.getTypeElement(FullyQualifiedNames.CATALOG_REQUIRED);
		if (catalogRequired == null) {
			return Optional.empty();
		}
		TypeElement catalogUnique = elements.getTypeElement(FullyQualifiedNames.CATALOG_UNIQUE);
		TypeElement catalogUniqueWrapper = elements.getTypeElement(FullyQualifiedNames.CATALOG_UNIQUE_PROPERTIES);
		TypeElement catalogFulltextIndex = elements.getTypeElement(FullyQualifiedNames.CATALOG_FULLTEXT);

		return Optional.of(new ElementsCatalog(catalogRequired, catalogUnique, catalogUniqueWrapper, catalogFulltextIndex));
	}
}
