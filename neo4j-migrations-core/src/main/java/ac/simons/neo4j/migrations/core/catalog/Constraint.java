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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.w3c.dom.Element;

/**
 * A somewhat Neo4j version independent representation of a constraint.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since 1.7.0
 */
public final class Constraint extends AbstractCatalogItem<Constraint.Type> {

	/**
	 * Enumerates the different kinds of constraints.
	 */
	public enum Type implements ItemType {

		/**
		 * Unique constraints.
		 */
		UNIQUE,
		/**
		 * Existential constraints.
		 */
		EXISTS,
		/**
		 * Key constraints.
		 */
		KEY;

		@Override
		public String getName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Programmatic way of defining constraints for nodes.
	 */
	public interface NodeConstraintBuilder {

		/**
		 * Adds a name to the constraint
		 *
		 * @param name The new name
		 * @return The next step when building a constraint
		 */
		ConstraintsForNodes named(String name);
	}

	/**
	 * Programmatic way of defining constraints for relationships.
	 */
	public interface RelationshipConstraintBuilder {

		/**
		 * Adds a name to the constraint
		 *
		 * @param name The new name
		 * @return The next step when building a constraint
		 */
		CommonConstraints named(String name);
	}

	/**
	 * Constraints available for both nodes and relationships.
	 */
	public interface CommonConstraints {

		/**
		 * Creates an existential constraint for the given property
		 *
		 * @param property the property that is required to exist
		 * @return the new constraint
		 */
		Constraint exists(String property);
	}

	/**
	 * Allows to specify the type of the constraint.
	 */
	public interface ConstraintsForNodes extends CommonConstraints {

		/**
		 * Creates a unique constraint for the given property
		 *
		 * @param properties the property that should be unique
		 * @return the new constraint
		 */
		Constraint unique(String... properties);

		/**
		 * Creates a key constraint for the given property
		 *
		 * @param properties the property that should be part of the key
		 * @return the new constraint
		 */
		Constraint key(String... properties);
	}

	private static class DefaultBuilder implements NodeConstraintBuilder, RelationshipConstraintBuilder, ConstraintsForNodes {
		private final TargetEntityType targetEntityType;

		private final String identifier;

		private String name;

		private DefaultBuilder(TargetEntityType targetEntityType, String identifier) {
			this.targetEntityType = targetEntityType;
			this.identifier = identifier;
		}

		@Override
		public ConstraintsForNodes named(String newName) {

			this.name = newName;
			return this;
		}

		@Override
		public Constraint unique(String... properties) {
			return new Constraint(name, Type.UNIQUE, targetEntityType, identifier, Arrays.asList(properties));
		}

		@Override
		public Constraint exists(String property) {
			return new Constraint(name, Type.EXISTS, targetEntityType, identifier, Collections.singleton(property));
		}

		@Override
		public Constraint key(String... properties) {
			return new Constraint(name, Type.KEY, targetEntityType, identifier, Arrays.asList(properties));
		}
	}

	/**
	 * Starts defining a new instance of a node constraint.
	 *
	 * @param label The label on which the constraint should be applied
	 * @return The ongoing builder
	 */
	public static NodeConstraintBuilder forNode(String label) {
		return new DefaultBuilder(TargetEntityType.NODE, label);
	}

	/**
	 * Starts defining a new instance of a relationship constraint.
	 *
	 * @param type The type on which the constraint should be applied
	 * @return The ongoing builder
	 */
	public static RelationshipConstraintBuilder forRelationship(String type) {
		return new DefaultBuilder(TargetEntityType.RELATIONSHIP, type);
	}

	/**
	 * Parses a constraint from a {@link MapAccessor}, which will either contain a result from {@code call db.constraints()}
	 * (Neo4j 3.5 upto 4.1) or from {@code SHOW CONSTRAINTS YIELD *} from Neo4j 4.2 and upwards
	 *
	 * @param row the result row
	 * @return A constraint
	 * @throws IllegalArgumentException if the row cannot be processed
	 */
	public static Constraint parse(MapAccessor row) {

		Value descriptionValue = row.get("description");
		Value nameValue = row.get("name");
		if (descriptionValue != Values.NULL) {
			String name = nameValue != Values.NULL ? nameValue.asString() : null;
			return parse(descriptionValue.asString(), name);
		}

		if (!REQUIRED_KEYS.stream().allMatch(row::containsKey)) {
			throw new IllegalArgumentException("Required keys are missing in the row describing the constraint");
		}

		String name = nameValue.asString();
		Constraint.Type type = switch (row.get(XMLSchemaConstants.TYPE).asString()) {
			case "NODE_KEY" -> Type.KEY;
			case "NODE_PROPERTY_EXISTENCE", "RELATIONSHIP_PROPERTY_EXISTENCE" -> Type.EXISTS;
			case "UNIQUENESS" -> Type.UNIQUE;
			default -> throw new IllegalArgumentException("Unsupported constraint type " + nameValue.asString());
		};

		TargetEntityType targetEntityType = TargetEntityType.valueOf(row.get("entityType").asString());
		List<String> labelsOrTypes = row.get("labelsOrTypes").asList(Value::asString);
		List<String> properties = row.get(XMLSchemaConstants.PROPERTIES).asList(Value::asString);
		String options = resolveOptions(row).orElse(null);

		return new Constraint(name, type, targetEntityType, labelsOrTypes.get(0), new LinkedHashSet<>(properties), options);
	}

	/**
	 * Creates a constraint from a xml definition.
	 *
	 * @param constraintElement as defined in {@code migration.xsd}.
	 * @return The new constraint if the element as parseable
	 */
	public static Constraint parse(Element constraintElement) {

		String name = constraintElement.getAttribute(XMLSchemaConstants.NAME);
		Type type = Type.valueOf(constraintElement.getAttribute(XMLSchemaConstants.TYPE).toUpperCase(Locale.ROOT));
		Target target = extractTarget(constraintElement);

		Set<String> properties = extractProperties(constraintElement);
		String options = extractOptions(constraintElement);

		if (target.targetEntityType() == TargetEntityType.RELATIONSHIP && type != Type.EXISTS) {
			throw new IllegalArgumentException("Only existential constraints are supported for relationships");
		}
		return new Constraint(name, type, target.targetEntityType(), target.identifier(), properties, options);
	}

	private static class PatternHolder {

		private final Pattern pattern;
		private final Constraint.Type type;
		private final TargetEntityType targetEntityType;

		PatternHolder(String pattern, Type type, TargetEntityType targetEntityType) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.type = type;
			this.targetEntityType = targetEntityType;
		}
	}

	private static final PatternHolder PATTERN_NODE_PROPERTY_IS_UNIQUE = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\(?\\k<var>\\.(?<properties>.+?)\\)? IS UNIQUE",
		Type.UNIQUE,
		TargetEntityType.NODE
	);

	private static final PatternHolder PATTERN_NODE_PROPERTY_EXISTS = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT (?:exists)?\\((?<properties>\\k<var>\\..+?)\\)(?: IS NOT NULL)?",
		Type.EXISTS,
		TargetEntityType.NODE
	);

	private static final PatternHolder PATTERN_NODE_KEY = new PatternHolder(
		"CONSTRAINT ON \\(\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?\\) ASSERT \\((?<properties>\\k<var>\\..+?)\\) IS NODE KEY",
		Type.KEY,
		TargetEntityType.NODE
	);

	private static final PatternHolder PATTERN_REL_PROPERTY_EXISTS = new PatternHolder(
		"CONSTRAINT ON \\(\\)-\\[\\s?(?<var>\\w+):(?<identifier>\\w+)\\s?]-\\(\\) ASSERT (?:exists)?\\((?<properties>\\k<var>\\..+?)\\)(?: IS NOT NULL)?",
		Type.EXISTS,
		TargetEntityType.RELATIONSHIP
	);

	private static final Set<String> REQUIRED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("name",
		XMLSchemaConstants.TYPE, "entityType", "labelsOrTypes", XMLSchemaConstants.PROPERTIES)));

	/**
	 * Creates a constraint from a [3.5, 4.1] database description
	 *
	 * @param description as returned by {@code CALL db.constraints}
	 * @param name        an optional name, might be {@literal null}
	 * @return The new constraint if the description is parseable
	 */
	private static Constraint parse(String description, String name) {

		for (PatternHolder patternHolder : new PatternHolder[] {
			PATTERN_NODE_PROPERTY_IS_UNIQUE,
			PATTERN_NODE_PROPERTY_EXISTS,
			PATTERN_NODE_KEY,
			PATTERN_REL_PROPERTY_EXISTS
		}) {
			Matcher matcher = patternHolder.pattern.matcher(description);
			if (!matcher.matches()) {
				continue;
			}

			String identifier = matcher.group("identifier").trim();
			String variable = Pattern.quote(matcher.group("var") + ".");
			String propertiesGroup = matcher.group(XMLSchemaConstants.PROPERTIES);
			Stream<String> propertiesStream = patternHolder.type == Type.KEY ?
				Arrays.stream(propertiesGroup.split(", ")).map(String::trim) :
				Stream.of(propertiesGroup.trim());
			String[] properties = propertiesStream.map(s -> s.replaceFirst(variable, "")).toArray(String[]::new);
			return new Constraint(name, patternHolder.type, patternHolder.targetEntityType, identifier,
				new LinkedHashSet<>(Arrays.asList(properties)));
		}

		throw new IllegalArgumentException(
			String.format("The description '%s' does not match any known pattern.", description));
	}

	Constraint(Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties) {
		this(null, type, targetEntityType, identifier, properties, null);
	}

	Constraint(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties) {
		this(name, type, targetEntityType, identifier, properties, null);
	}

	Constraint(String name, Type type, TargetEntityType targetEntityType, String identifier, Collection<String> properties, String options) {
		super(name, type, targetEntityType, identifier, properties, options);

		if (type == Type.KEY && getTargetEntityType() != TargetEntityType.NODE) {
			throw new IllegalArgumentException("Key constraints are only supported for nodes, not for relationships.");
		}
	}

	/**
	 * Creates a copy of this constraint with the specific set of options added. Will return {@literal this} instance if
	 * the options are identical to current options
	 *
	 * @param options The new options to use
	 * @return A (potentially) new constraint
	 * @since 1.13.0
	 */
	@SuppressWarnings("HiddenField")
	public Constraint withOptions(String options) {

		if (Objects.equals(super.options, options)) {
			return this;
		}

		return new Constraint(getName().getValue(), getType(), getTargetEntityType(), getIdentifier(), getProperties(),
			options);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public Constraint withName(String name) {

		if (Objects.equals(super.getName().getValue(), name)) {
			return this;
		}

		return new Constraint(name, getType(), getTargetEntityType(), getIdentifier(), getProperties(), options);
	}

	/**
	 * {@literal true}, if {@literal item} is a constraint of the same type for the same entity containing the same properties.
	 *
	 * @param that the other item to compare to
	 * @return {@literal true} if this and the other {@literal item} are equivalent
	 */
	@Override
	public boolean isEquivalentTo(CatalogItem<?> that) {

		if (this == that) {
			return true;
		}

		if (!(that instanceof Constraint)) {
			return false;
		}

		Constraint other = (Constraint) that;

		return this.getType().equals(other.getType()) &&
			this.getTargetEntityType().equals(other.getTargetEntityType()) &&
			this.getIdentifier().equals(other.getIdentifier()) &&
			this.getProperties().equals(other.getProperties());
	}
}
