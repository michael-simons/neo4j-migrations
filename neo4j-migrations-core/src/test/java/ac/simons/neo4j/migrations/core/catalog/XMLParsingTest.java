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

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.MigrationsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 * @author Michael J. Simons
 */
class XMLParsingTest {

	private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = ThreadLocal.withInitial(() -> {
		DocumentBuilderFactory value = DocumentBuilderFactory.newInstance();
		value.setExpandEntityReferences(false);
		value.setNamespaceAware(true);
		return value;
	});

	private static Catalog load(URL url) {
		try (InputStream source = url.openStream()) {
			DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
			return Catalog.of(documentBuilder.parse(source));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new MigrationsException("Could not parse the given document.", e);
		}
	}

	@Test
	void shouldFindAllConstraints() {
		Catalog catalog = load(XMLParsingTest.class.getResource("/xml/parsing/full-example.xml"));
		assertThat(catalog.getItems().stream().filter(Constraint.class::isInstance)
			.map(Constraint.class::cast))
			.extracting(Constraint::getName)
			.containsExactlyInAnyOrder(Name.of("unique_isbn"), Name.of("exists_isbn"), Name.of("old_keys"));
	}
}
