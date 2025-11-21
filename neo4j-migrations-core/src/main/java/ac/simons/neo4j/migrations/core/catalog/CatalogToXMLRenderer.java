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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ac.simons.neo4j.migrations.core.catalog.RenderConfig.XMLRenderingOptions;
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;
import ac.simons.neo4j.migrations.core.internal.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Renders a catalog into XML.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
@SuppressWarnings("squid:S6548")
enum CatalogToXMLRenderer implements Renderer<Catalog> {

	INSTANCE;

	@Override
	public void render(Catalog catalog, RenderConfig config, OutputStream target) throws IOException {

		List<XMLRenderingOptions> relevantOptions = config.getAdditionalOptions()
			.stream()
			.filter(XMLRenderingOptions.class::isInstance)
			.map(XMLRenderingOptions.class::cast)
			.toList();
		boolean withApply = relevantOptions.stream()
			.map(XMLRenderingOptions::withApply)
			.reduce(!relevantOptions.isEmpty(), (v1, v2) -> v1 && v2);
		boolean withReset = relevantOptions.stream()
			.map(XMLRenderingOptions::withReset)
			.reduce(!relevantOptions.isEmpty(), (v1, v2) -> v1 && v2);

		String header = relevantOptions.stream()
			.map(XMLRenderingOptions::optionalHeader)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.joining(System.lineSeparator(), " ", " "));

		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			Element migrationElement = document.createElementNS(XMLSchemaConstants.MIGRATION_NS,
					XMLSchemaConstants.MIGRATION);

			if (!header.trim().isEmpty()) {
				migrationElement.appendChild(document.createComment(header));
			}
			document.appendChild(migrationElement);

			Element catalogElement = document.createElement(XMLSchemaConstants.CATALOG);
			if (withReset) {
				catalogElement.setAttribute(XMLSchemaConstants.RESET, Boolean.toString(true));
			}

			Element indexesElement = document.createElement(XMLSchemaConstants.INDEXES);
			migrationElement.appendChild(catalogElement);
			catalogElement.appendChild(indexesElement);

			Element constraintsElement = document.createElement(XMLSchemaConstants.CONSTRAINTS);
			catalogElement.appendChild(constraintsElement);

			for (CatalogItem<?> item : catalog.getItems()) {
				if (item instanceof Constraint constraint) {
					constraintsElement.appendChild(constraint.toXML(document));
				}
				else if (item instanceof Index index) {
					indexesElement.appendChild(index.toXML(document));
				}
			}

			if (withApply) {
				Element apply = document.createElement(XMLSchemaConstants.APPLY);
				migrationElement.appendChild(apply);
			}

			XMLUtils.getIndentingTransformer().transform(new DOMSource(document), new StreamResult(target));
		}
		catch (ParserConfigurationException | TransformerException ex) {
			throw new IOException(ex);
		}
	}

}
