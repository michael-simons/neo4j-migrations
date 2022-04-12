package ac.simons.neo4j.migrations.core.schema;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Demo {

	public static void main(String... a)
		throws SAXException, IOException, ParserConfigurationException {

		SchemaFactory factory =
			SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(
			new StreamSource(Demo.class.getResourceAsStream("/schema.xsd")));

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setSchema(schema);
		documentBuilderFactory.setNamespaceAware(true);
		Document parse = documentBuilderFactory.newDocumentBuilder()
			.parse(Demo.class.getResourceAsStream("/bookConstraints.xml"));

		NodeList constraints = parse.getElementsByTagName("constraint");
		for (int i = 0; i < constraints.getLength(); ++i) {
			Element item = (Element) constraints.item(i);
			Constraint constraint = Constraint.item(item);
			System.out.println(constraint.to35());
			System.out.println(constraint.to40());
			System.out.println(constraint.to41());
			System.out.println(constraint.to42());
			System.out.println(constraint.to43());
			System.out.println(constraint.to44());
		}
	}
}
