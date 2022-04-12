package ac.simons.neo4j.migrations.core.catalog;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Constraint {

	public static Constraint of(Element constraintElement) {

		String name = constraintElement.getAttribute("id");
		Type type = Type.valueOf(constraintElement.getAttribute("type").toUpperCase(Locale.ROOT));
		NodeList labelOrType = constraintElement.getElementsByTagName("label");
		Target target;
		String identifier;
		if (labelOrType.getLength() == 0) {
			labelOrType = constraintElement.getElementsByTagName("type");
			target = Target.RELATIONSHIP;
		} else {
			target = Target.NODE;
		}
		identifier = labelOrType.item(0).getTextContent();

		NodeList propertyNodes = ((Element) constraintElement
			.getElementsByTagName("properties").item(0)).getElementsByTagName("property");
		Set<String> properties = new HashSet<>();
		for (int i = 0; i < propertyNodes.getLength(); ++i) {
			properties.add(propertyNodes.item(i).getTextContent());
		}

		return new Constraint(name, type, target, identifier, properties);
	}

	enum Type {
		UNIQUE,
		EXISTS,
		KEY
	}

	enum Target {
		NODE,
		RELATIONSHIP
	}

	private final String name;

	private final Type type;

	private final Target target;

	private final String identifier;

	private final Set<String> properties;

	private Constraint(String name, Type type, Target target, String identifier, Set<String> properties) {
		this.name = name;
		this.type = type;
		this.target = target;
		this.identifier = identifier;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public Target getTarget() {
		return target;
	}

	public String getIdentifier() {
		return identifier;
	}

	/*
	public String to35() {

		return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT n.%s IS %s", identifier, properties.iterator().next(),
			type);
	}

	public String to40() {
		return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT n.%s IS %s", name, identifier,
			properties.iterator().next(), type);
	}

	public String to41() {
		return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT n.%s IS %s", name,
			idempotent ? "IF NOT EXISTS " : "", identifier,
			properties.iterator().next(), type);
	}

	public String to42() {
		return to41();
	}

	public String to43() {
		return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT n.%s IS %s", name,
			idempotent ? "IF NOT EXISTS " : "", identifier,
			properties.iterator().next(), type);
	}

	public String to44() {
		return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE n.%s IS %s", name,
			idempotent ? "IF NOT EXISTS " : "", identifier,
			properties.iterator().next(), type);
	}*/

	@Override public String toString() {
		return "Constraint{" +
			   "name='" + name + '\'' +
			   ", type=" + type +
			   ", target=" + target +
			   ", identifier='" + identifier + '\'' +
			   '}';
	}
}
