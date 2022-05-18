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

import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Formattable;
import java.util.stream.Collectors;

/**
 * Renders constraints (supported operators are {@link Operator#CREATE} and {@link Operator#DROP}) as Cypher.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
enum ConstraintToCypherRenderer implements Renderer<Constraint> {

	INSTANCE;

	@Override
	public void render(Constraint constraint, RenderConfig context, OutputStream target) throws IOException {

		Neo4jVersion version = context.getVersion();
		if (context.isIdempotent() && Neo4jVersion.NO_IDEM_POTENCY.contains(version) && !context.isIgnoreName()) {
			throw new IllegalStateException(
				String.format("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.",
					version));
		}

		if (constraint.getProperties().size() > 1) {
			if (!EnumSet.of(Constraint.Type.UNIQUE, Constraint.Type.KEY).contains(constraint.getType())) {
				throw new IllegalStateException("Only unique and node key constraints support multiple properties.");
			}

			if (context.isVersionPriorTo44() && constraint.getType() != Constraint.Type.KEY) {
				throw new IllegalStateException("Constraints require exactly one property prior to Neo4j 4.4.");
			}
		}

		if (!constraint.hasName() && context.isIdempotent() && context.getOperator() == Operator.DROP) {
			throw new IllegalStateException("The constraint can only be rendered in the given context when having a name.");
		}

		Writer w = new BufferedWriter(new OutputStreamWriter(target, StandardCharsets.UTF_8));
		if (context.getOperator() == Operator.DROP && context.getVersion() != Neo4jVersion.V3_5 && constraint.hasName() && !context.isIgnoreName()) {
			w.write(String.format("DROP %#s%s", constraint, ifNotExistsOrEmpty(context)));
		} else {
			switch (constraint.getType()) {
				case UNIQUE:
					w.write(renderUniqueConstraint(constraint, context));
					break;
				case EXISTS:
					w.write(renderPropertyExists(constraint, context));
					break;
				case KEY:
					w.write(renderNodeKey(constraint, context));
					break;
				default:
					throw new IllegalArgumentException("Unsupported type of constraint: " + constraint.getType());
			}
		}
		w.flush();
	}

	private String renderNodeKey(Constraint constraint, RenderConfig context) {

		if (context.getOperator() == Operator.CREATE && context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be created with %s edition.", context.getEdition()));
		}

		Formattable item = formattableItem(constraint, context);
		String identifier = constraint.getIdentifier();
		String properties = renderProperties("n", constraint);
		Operator operator = context.getOperator();

		Neo4jVersion version = context.getVersion();
		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON (n:%s) ASSERT %s IS NODE KEY", operator, item, identifier, properties);
		} else if (Neo4jVersion.RANGE_41_TO_43.contains(version)) {
			return String.format("%s %#s %sON (n:%s) ASSERT %s IS NODE KEY", operator, item,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s %#s %s%s (n:%s) %s %s IS NODE KEY", operator, item,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, properties);
		}
	}

	private String renderPropertyExists(Constraint item, RenderConfig context) {

		if (context.getOperator() == Operator.CREATE && context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be created with %s edition.", context.getEdition()));
		}

		if (item.getTarget() == TargetEntity.NODE) {
			return renderNodePropertyExists(item, context);
		}
		return renderRelationshipPropertyExists(item, context);
	}

	private String renderRelationshipPropertyExists(Constraint constraint, RenderConfig context) {

		Formattable item = formattableItem(constraint, context);
		String identifier = constraint.getIdentifier();
		String properties = renderProperties("r", constraint);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		Neo4jVersion version = context.getVersion();
		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON ()-[r:%s]-() ASSERT exists(%s)", operator, item, identifier, properties);
		} else if (Neo4jVersion.RANGE_41_TO_42.contains(version)) {
			return String.format("%s %#s %sON ()-[r:%s]-() ASSERT exists(%s)", operator, item,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Neo4jVersion.V4_3) {
			return String.format("%s %#s %sON ()-[r:%s]-() ASSERT %s", operator, item,
				ifNotExistsOrEmpty(context), identifier, object);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s %#s %s%s ()-[r:%s]-() %s %s", operator, item,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, object);
		}
	}

	private String renderNodePropertyExists(Constraint constraint, RenderConfig context) {

		Formattable item = formattableItem(constraint, context);
		String identifier = constraint.getIdentifier();
		String properties = renderProperties("n", constraint);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		Neo4jVersion version = context.getVersion();
		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON (n:%s) ASSERT exists(%s)", operator, item, identifier, properties);
		} else if (Neo4jVersion.RANGE_41_TO_42.contains(version)) {
			return String.format("%s %#s %sON (n:%s) ASSERT exists(%s)", operator, item,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Neo4jVersion.V4_3) {
			return String.format("%s %#s %sON (n:%s) ASSERT %s", operator, item,
				ifNotExistsOrEmpty(context), identifier, object);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s %#s %s%s (n:%s) %s %s", operator, item,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, object);
		}
	}

	private static String ifNotExistsOrEmpty(RenderConfig context) {
		switch (context.getOperator()) {
			case CREATE:
				return context.isIdempotent() ? "IF NOT EXISTS " : "";
			case DROP:
				return context.isIdempotent() ? " IF EXISTS" : "";
		}
		throw new IllegalStateException();
	}

	private String renderUniqueConstraint(Constraint constraint, RenderConfig context) {

		Formattable item = formattableItem(constraint, context);
		String identifier = constraint.getIdentifier();
		String properties = renderProperties("n", constraint);
		Constraint.Type type = constraint.getType();
		Operator operator = context.getOperator();

		Neo4jVersion version = context.getVersion();
		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON (n:%s) ASSERT %s IS %s", operator, item, identifier, properties, type);
		} else if (Neo4jVersion.RANGE_41_TO_43.contains(version)) {
			return String.format("%s %#s %sON (n:%s) ASSERT %s IS %s", operator, item, ifNotExistsOrEmpty(context),
				identifier, properties, type);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s %#s %s%s (n:%s) %s %s IS %s", operator, item,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, properties, type);
		}
	}

	private static String renderProperties(String prefix, Constraint item) {

		if (item.getProperties().size() == 1) {
			return prefix + "." + item.getProperties().iterator().next();
		}

		return item.getProperties().stream().map(v -> prefix + "." + v).collect(Collectors.joining(", ", "(", ")"));
	}

	Formattable formattableItem(Constraint item, RenderConfig config) {
		return config.isIgnoreName() || config.getVersion() == Neo4jVersion.V3_5 ? new AnonymousCatalogItem(item) : item;
	}
}
