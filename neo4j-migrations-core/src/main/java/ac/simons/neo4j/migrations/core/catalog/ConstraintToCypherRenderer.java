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
	public void render(Constraint item, RenderContext context, OutputStream target) throws IOException {

		Neo4jVersion version = context.getVersion();
		if (context.isIdempotent() && Neo4jVersion.NO_IDEM_POTENCY.contains(version)) {
			throw new IllegalStateException(
				String.format("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.",
					version));
		}

		if (item.getProperties().size() > 1) {
			if (!EnumSet.of(Constraint.Type.UNIQUE, Constraint.Type.KEY).contains(item.getType())) {
				throw new IllegalStateException("Only unique and node key constraints support multiple properties.");
			}

			if (context.isVersionPriorTo44() && item.getType() != Constraint.Type.KEY) {
				throw new IllegalStateException("Constraints require exactly one property prior to Neo4j 4.4.");
			}
		}

		if (!item.hasName() && context.isIdempotent() && context.getOperator() == Operator.DROP) {
			throw new IllegalStateException("The constraint can only be rendered in the given context when having a name.");
		}

		Writer w = new BufferedWriter(new OutputStreamWriter(target, StandardCharsets.UTF_8));
		if (context.getOperator() == Operator.DROP && context.getVersion() != Neo4jVersion.V3_5 && item.hasName() && !context.isIgnoreName()) {
			w.write(String.format("DROP CONSTRAINT%s%s", item.getName(), ifNotExistsOrEmpty(context)));
		} else {
			switch (item.getType()) {
				case UNIQUE:
					w.write(renderUniqueConstraint(item, context));
					break;
				case EXISTS:
					w.write(renderPropertyExists(item, context));
					break;
				case KEY:
					w.write(renderNodeKey(item, context));
					break;
				default:
					throw new IllegalArgumentException("Unsupported type of constraint: " + item.getType());
			}
		}
		w.flush();
	}

	private String renderNodeKey(Constraint item, RenderContext context) {

		if (context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() != TargetEntity.NODE) {
			throw new IllegalStateException("Key constraints are only supported for nodes, not for relationships.");
		}

		Name name = getNameOrEmpty(item, context);
		Neo4jVersion version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);

		if (version == Neo4jVersion.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT %s IS NODE KEY", context.getOperator(), identifier, properties);
		} else if (version == Neo4jVersion.V4_0) {
			return String.format("CREATE CONSTRAINT%s ON (n:%s) ASSERT %s IS NODE KEY", name, identifier, properties);
		} else if (Neo4jVersion.RANGE_41_TO_43.contains(version)) {
			return String.format("CREATE CONSTRAINT%s %sON (n:%s) ASSERT %s IS NODE KEY", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT%s %sFOR (n:%s) REQUIRE %s IS NODE KEY", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		}
	}

	private String renderPropertyExists(Constraint item, RenderContext context) {

		if (context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() == TargetEntity.NODE) {
			return renderNodePropertyExists(item, context);
		}
		return renderRelationshipPropertyExists(item, context);
	}

	private String renderRelationshipPropertyExists(Constraint item, RenderContext context) {

		Name name = getNameOrEmpty(item, context);
		Neo4jVersion version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("r", item);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		if (version == Neo4jVersion.V3_5) {
			return String.format("%s CONSTRAINT ON ()-[r:%s]-() ASSERT exists(%s)", operator, identifier, properties);
		} else if (version == Neo4jVersion.V4_0) {
			return String.format("%s CONSTRAINT%s ON ()-[r:%s]-() ASSERT exists(%s)", operator, name, identifier,
				properties);
		} else if (Neo4jVersion.RANGE_41_TO_42.contains(version)) {
			return String.format("%s CONSTRAINT%s %sON ()-[r:%s]-() ASSERT exists(%s)", operator, name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Neo4jVersion.V4_3) {
			return String.format("%s CONSTRAINT%s %sON ()-[r:%s]-() ASSERT %s", operator, name,
				ifNotExistsOrEmpty(context), identifier, object);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s CONSTRAINT%s %s%s ()-[r:%s]-() %s %s", operator, name,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, object);
		}
	}

	private String renderNodePropertyExists(Constraint item, RenderContext context) {

		Name name = getNameOrEmpty(item, context);
		Neo4jVersion version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		if (version == Neo4jVersion.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT exists(%s)", operator, identifier, properties);
		} else if (version == Neo4jVersion.V4_0) {
			return String.format("%s CONSTRAINT%s ON (n:%s) ASSERT exists(%s)", operator, name, identifier, properties);
		} else if (Neo4jVersion.RANGE_41_TO_42.contains(version)) {
			return String.format("%s CONSTRAINT%s %sON (n:%s) ASSERT exists(%s)", operator, name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Neo4jVersion.V4_3) {
			return String.format("%s CONSTRAINT%s %sON (n:%s) ASSERT %s", operator, name,
				ifNotExistsOrEmpty(context), identifier, object);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s CONSTRAINT%s %s%s (n:%s) %s %s", operator, name,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, object);
		}
	}

	private static String ifNotExistsOrEmpty(RenderContext context) {
		switch (context.getOperator()) {
			case CREATE:
				return context.isIdempotent() ? "IF NOT EXISTS " : "";
			case DROP:
				return context.isIdempotent() ? " IF EXISTS" : "";
		}
		throw new IllegalStateException();
	}

	private String renderUniqueConstraint(Constraint item, RenderContext context) {

		Name name = getNameOrEmpty(item, context);
		Neo4jVersion version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);
		Constraint.Type type = item.getType();
		Operator operator = context.getOperator();

		if (version == Neo4jVersion.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT %s IS %s", operator, identifier,
				properties, type);
		} else if (version == Neo4jVersion.V4_0) {
			return String.format("%s CONSTRAINT%s ON (n:%s) ASSERT %s IS %s", operator, name, identifier, properties, type);
		} else if (Neo4jVersion.RANGE_41_TO_43.contains(version)) {
			return String.format("%s CONSTRAINT%s %sON (n:%s) ASSERT %s IS %s", operator, name, ifNotExistsOrEmpty(context),
				identifier, properties, type);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? "REQUIRE" : "ASSERT";
			// We just assume the newest
			return String.format("%s CONSTRAINT%s %s%s (n:%s) %s %s IS %s", operator, name,
				ifNotExistsOrEmpty(context), adjective, identifier, verb, properties, type);
		}
	}

	private static String renderProperties(String prefix, Constraint item) {

		if (item.getProperties().size() == 1) {
			return prefix + "." + item.getProperties().iterator().next();
		}

		return item.getProperties().stream().map(v -> prefix + "." + v).collect(Collectors.joining(", ", "(", ")"));
	}

	private Name getNameOrEmpty(Constraint item, RenderContext context) {
		return context.isIgnoreName() ? Name.empty() : item.getName();
	}
}
