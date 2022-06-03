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

import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Represents a schema of
 * <ul>
 *     <li>Constraints</li>
 *     <li>Indizes</li>
 * </ul>
 * and probably other things in the future to come. The complete schema of a {@link ac.simons.neo4j.migrations.core.MigrationContext migration context}
 * will be made up of all schema entries found in the migrations discovered. schema entries of the same type and with the
 * same id will all live up in the corresponding bucket so that for example a drop operation can refer to an older version
 * of a constraint to be dropped, either explicitly or implicit.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since TBA
 */
@FunctionalInterface
public interface Catalog {

	/**
	 * Creates a catalog based on a document following the schema of {@literal /ac/simons/neo4j/migrations/core/migration.xsd}.
	 *
	 * @param document A document containing a catalog element and / or one or more nested catalog item elements.
	 * @return A new catalog, containing all items that can be extracted from the document
	 */
	static Catalog of(Document document) {

		NodeList catalog = document.getElementsByTagName(XMLSchemaConstants.CATALOG);
		if (catalog.getLength() > 1) {
			throw new IllegalArgumentException("More than one catalog item found.");
		}

		List<CatalogItem<?>> items = new ArrayList<>();
		if (catalog.getLength() == 0) {
			return Catalog.empty();
		}

		NodeList constraintNodeList = ((Element) catalog.item(0)).getElementsByTagName(XMLSchemaConstants.CONSTRAINT);
		for (int i = 0; i < constraintNodeList.getLength(); ++i) {
			Element item = (Element) constraintNodeList.item(i);
			items.add(Constraint.parse(item));
		}

		NodeList indexNodeList = ((Element) catalog.item(0)).getElementsByTagName(XMLSchemaConstants.INDEX);
		for (int i = 0; i < indexNodeList.getLength(); ++i) {
			Element item = (Element) indexNodeList.item(i);
			items.add(Index.parse(item));
		}

		return new CatalogImpl(items);
	}

	/**
	 * Creates a catalog based on the given collection.
	 *
	 * @param items A collection of items
	 * @return A new catalog
	 */
	static Catalog of(Collection<CatalogItem<?>> items) {
		return new CatalogImpl(items);
	}

	/**
	 * @return an empty catalog
	 */
	static Catalog empty() {
		return EmptyCatalog.INSTANCE;
	}

	/**
	 * @return the list of all items in this catalog
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	Collection<CatalogItem<?>> getItems();

	/**
	 * Helper method to check weather this catalog contains an equivalent item
	 *
	 * @param other The item for which an equivalent is search
	 * @return True if an equivalent item exists
	 */
	default boolean containsEquivalentItem(CatalogItem<?> other) {
		return this.getItems().stream().anyMatch(item -> item.isEquivalentTo(other));
	}

	/**
	 * @return {@literal true} if this catalog is empty.
	 */
	default boolean isEmpty() {
		return this.getItems().isEmpty();
	}
}
