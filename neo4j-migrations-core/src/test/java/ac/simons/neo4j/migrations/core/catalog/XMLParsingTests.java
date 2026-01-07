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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.internal.ThrowingErrorHandler;
import ac.simons.neo4j.migrations.test_resources.TestResources;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class XMLParsingTests {

	private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = ThreadLocal.withInitial(() -> {
		DocumentBuilderFactory value = DocumentBuilderFactory.newInstance();
		value.setExpandEntityReferences(false);
		value.setNamespaceAware(true);
		return value;
	});

	private static final ThreadLocal<DocumentBuilderFactory> VALIDATING_DOCUMENT_BUILDER_FACTORY = ThreadLocal
		.withInitial(() -> {

			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			DocumentBuilderFactory value = DocumentBuilderFactory.newInstance();
			value.setExpandEntityReferences(false);
			value.setNamespaceAware(true);
			try {
				Schema schema = schemaFactory.newSchema(new StreamSource(
						XMLParsingTests.class.getResourceAsStream("/ac/simons/neo4j/migrations/core/migration.xsd")));
				value.setSchema(schema);
			}
			catch (SAXException ex) {
				throw new RuntimeException(ex);
			}
			return value;
		});

	private static Catalog load(URL url, boolean validate) {
		try (InputStream source = url.openStream()) {
			DocumentBuilder documentBuilder;
			if (validate) {
				documentBuilder = VALIDATING_DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
				documentBuilder.setErrorHandler(new ThrowingErrorHandler());
			}
			else {
				documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
			}
			return Catalog.of(documentBuilder.parse(source));
		}
		catch (SAXException | IOException | ParserConfigurationException ex) {
			throw new MigrationsException("Could not parse the given document.", ex);
		}
	}

	@Test
	void shouldFindAllConstraintsAndIndexes() {
		Catalog catalog = load(TestResources.class.getResource("/catalogbased/parsing/full-example.xml"), true);
		assertThat(catalog.getItems().stream().filter(Constraint.class::isInstance).map(Constraint.class::cast))
			.extracting(Constraint::getName)
			.containsExactlyInAnyOrder(Name.of("unique_isbn"), Name.of("exists_isbn"), Name.of("xyz"),
					Name.of("unique_role"), Name.of("prop_type"));

		assertThat(catalog.getItems().stream().filter(Constraint.class::isInstance).map(Constraint.class::cast))
			.extracting(Constraint::getType)
			.contains(Constraint.Type.UNIQUE, Constraint.Type.EXISTS, Constraint.Type.PROPERTY_TYPE,
					Constraint.Type.UNIQUE_RELATIONSHIP_PROPERTY);

		assertThat(catalog.getItems()
			.stream()
			.filter(Constraint.class::isInstance)
			.map(Constraint.class::cast)
			.filter(c -> c.getType() == Constraint.Type.PROPERTY_TYPE)).map(Constraint::getPropertyType)
			.contains(PropertyType.LOCAL_DATETIME);

		assertThat(catalog.getItems().stream().filter(Index.class::isInstance).map(Index.class::cast))
			.extracting(Index::getName)
			.containsExactlyInAnyOrder(Name.of("reads_index"), Name.of("title_index"), Name.of("metrics"));

		assertThat(catalog.getItems().stream().filter(Index.class::isInstance).map(Index.class::cast))
			.extracting(Index::getType)
			.containsExactlyInAnyOrder(Index.Type.FULLTEXT, Index.Type.PROPERTY, Index.Type.PROPERTY);
	}

	@Test
	void shouldNotCreateInvalidConstraints() {

		URL resource = TestResources.class.getResource("/catalogbased/parsing/invalid_constraint.xml");
		Objects.requireNonNull(resource);
		assertThatIllegalArgumentException().isThrownBy(() -> load(resource, false));
	}

	@Test // GH-1011
	void shouldNotCreateInvalidPropertyTypeConstraintsWithMultipleProps() {

		URL resource = TestResources.class.getResource("/catalogbased/parsing/invalid_constraint2.xml");
		Objects.requireNonNull(resource);
		assertThatIllegalArgumentException().isThrownBy(() -> load(resource, false))
			.withMessage("Only one property allowed on property type constraints.");
	}

	@Test // GH-1011
	void shouldNotCreateInvalidPropertyTypeConstraintsWithoutType() {

		URL resource = TestResources.class.getResource("/catalogbased/parsing/invalid_constraint3.xml");
		Objects.requireNonNull(resource);
		assertThatIllegalArgumentException().isThrownBy(() -> load(resource, false))
			.withMessage("The type attribute for properties is required on property type constraints.");
	}

	@Test // GH-1011
	void shouldNotCreateInvalidPropertyTypeConstraintsWithBlankType() {

		URL resource = TestResources.class.getResource("/catalogbased/parsing/invalid_constraint4.xml");
		Objects.requireNonNull(resource);
		assertThatIllegalArgumentException().isThrownBy(() -> load(resource, false))
			.withMessage("The type attribute for properties is required on property type constraints.");
	}

	@Test
	void indexShouldParseMultilabelFulltextIndex() throws ParserConfigurationException, IOException, SAXException {

		DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
		String xml = """
				<index name="title_index" type="fulltext">
				    <label>Book|Buch|Wurst\\|Salat</label>
				    <properties>
				        <property>title</property>
				    </properties>
				</index>""";
		Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element indexElement = document.getDocumentElement();
		Index index = Index.parse(indexElement);
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("Book", "Buch", "Wurst|Salat");
		assertThat(index.getIdentifier()).isEqualTo("[Book, Buch, Wurst|Salat]");
	}

	@Test
	void indexShouldNotParseMultilabelPropertyIndex() throws ParserConfigurationException, IOException, SAXException {

		DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
		String xml = """
				<index name="title_index">
				    <label>Book|Buch|Wurst\\|Salat</label>
				    <properties>
				        <property>title</property>
				    </properties>
				</index>""";
		Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element indexElement = document.getDocumentElement();
		assertThatIllegalArgumentException().isThrownBy(() -> Index.parse(indexElement))
			.withMessage("Multiple labels or types are only allowed to be specified with fulltext indexes.");
	}

	@Test
	void indexShouldNotParseMultiplePropertiesForTextIndexes()
			throws ParserConfigurationException, IOException, SAXException {

		DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
		String xml = """
				<index name="title_index" type="text">
				    <label>Person</label>
				    <properties>
				        <property>firstname</property>
				        <property>lastname</property>
				    </properties>
				</index>""";
		Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element indexElement = document.getDocumentElement();
		assertThatIllegalArgumentException().isThrownBy(() -> Index.parse(indexElement))
			.withMessage("Text indexes only allow exactly one single property.");
	}

}
