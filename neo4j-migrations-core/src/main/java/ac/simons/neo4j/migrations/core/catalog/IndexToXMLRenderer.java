/*
 * Copyright 2020-2026 the original author or authors.
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ac.simons.neo4j.migrations.core.internal.XMLUtils;
import org.w3c.dom.Document;

/**
 * Renders indexes as XML.
 *
 * @author Gerrit Meier
 */
@SuppressWarnings("squid:S6548")
enum IndexToXMLRenderer implements Renderer<Index> {

	INSTANCE;

	@Override
	public void render(Index item, RenderConfig context, OutputStream target) throws IOException {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			document.appendChild(item.toXML(document));

			Transformer transformer = XMLUtils.getIndentingTransformer();
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(target);

			transformer.transform(domSource, streamResult);
		}
		catch (ParserConfigurationException | TransformerException ex) {
			throw new IOException(ex);
		}
	}

}
