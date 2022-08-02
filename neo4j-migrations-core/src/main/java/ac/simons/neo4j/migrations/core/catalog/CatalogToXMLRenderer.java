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
import ac.simons.neo4j.migrations.core.internal.XMLUtils;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Michael J. Simons
 * @since 1.7.0
 */
enum CatalogToXMLRenderer implements Renderer<Catalog> {

	INSTANCE;

	@Override
	public void render(Catalog catalog, RenderConfig config, OutputStream target) throws IOException {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			Element migrationElement = document.createElementNS(XMLSchemaConstants.MIGRATION_NS,
				XMLSchemaConstants.MIGRATION);
			document.appendChild(migrationElement);

			Element catalogElement = document.createElement(XMLSchemaConstants.CATALOG);
			migrationElement.appendChild(catalogElement);

			Element indexesElement = document.createElement(XMLSchemaConstants.INDEXES);
			catalogElement.appendChild(indexesElement);

			Element constraintsElement = document.createElement(XMLSchemaConstants.CONSTRAINTS);
			catalogElement.appendChild(constraintsElement);

			for (CatalogItem<?> item : catalog.getItems()) {
				if (item instanceof Constraint) {
					constraintsElement.appendChild(((Constraint) item).toXML(document));
				} else if (item instanceof Index) {
					indexesElement.appendChild(((Index) item).toXML(document));
				}
			}

			XMLUtils.getIndentingTransformer().transform(new DOMSource(document), new StreamResult(target));
		} catch (ParserConfigurationException | TransformerException e) {
			throw new IOException(e);
		}
	}
}
