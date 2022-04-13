package ac.simons.neo4j.migrations.core.catalog;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlMigrationTest {
	public static void main(String... a)
		throws SAXException, IOException, ParserConfigurationException {

		SchemaFactory factory =
			SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(
			new StreamSource(
				XmlMigrationTest.class.getResourceAsStream("/ac/simons/neo4j/migrations/core/migration.xsd")));

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setSchema(schema);
		documentBuilderFactory.setExpandEntityReferences(false);
		documentBuilderFactory.setNamespaceAware(true);

		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		documentBuilder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				throw exception;
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				if (!exception.getMessage().startsWith("cvc-id.1")) {
					throw exception;
				}
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				throw exception;
			}
		});
		Document parse = documentBuilder
			.parse(XmlMigrationTest.class.getResourceAsStream("/xml/simple/V001__simple_catalog.xml"));

		NodeList constraints = parse.getElementsByTagName("constraint");
		for (int i = 0; i < constraints.getLength(); ++i) {
			Element item = (Element) constraints.item(i);
			Constraint constraint = Constraint.of(item);
			/*
			System.out.println(constraint.to35());
			System.out.println(constraint.to40());
			System.out.println(constraint.to41());
			System.out.println(constraint.to42());
			System.out.println(constraint.to43());
			System.out.println(constraint.to44());*/
		}

		NodeList drops = parse.getElementsByTagName("drop");
		for (int i = 0; i < drops.getLength(); ++i) {
			Element item = (Element) drops.item(i);

			/*
			// if (item.getAttribute("item") != null)
			//				System.out.println(Constraint.of(parse.getElementById(item.getAttribute("item"))));
			*/
			if (!item.getAttribute("ref").isEmpty()) {
				Constraint ref = Constraint.of(parse.getElementById(item.getAttribute("ref")));

				System.out.println(ref);
				ref.getOptionalOptions().ifPresent(System.out::println);
			}

		}
	}
}
