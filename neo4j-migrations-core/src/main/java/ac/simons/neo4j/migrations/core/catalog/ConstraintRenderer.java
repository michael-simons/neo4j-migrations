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

import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.Neo4jEdition;

import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Renders constraints (supported operators are {@link Operator#CREATE} and {@link Operator#DROP}).
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
final class ConstraintRenderer implements Renderer<Constraint> {

	@Override
	public String render(Constraint item, RenderContext context) {

		Version version = context.getVersion();
		if (context.isIdempotent() && Version.NO_IDEM_POTENCY.contains(version)) {
			throw new MigrationsException(
				String.format("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.",
					version));
		}

		if (item.getProperties().size() > 1) {
			if (!EnumSet.of(Constraint.Type.UNIQUE, Constraint.Type.KEY).contains(item.getType())) {
				throw new MigrationsException("Only unique and node key constraints support multiple properties.");
			}

			if (context.isVersionPriorTo44() && item.getType() != Constraint.Type.KEY) {
				throw new MigrationsException("Constraints require exactly one property prior to Neo4j 4.4.");
			}
		}

		if (!item.hasName() && context.isIdempotent() && context.getOperator() == Operator.DROP) {
			throw new MigrationsException("The constraint can only be rendered in the given context when having a name.");
		}

		String f;
		if (context.getOperator() == Operator.DROP && context.getVersion() != Version.V3_5 && item.hasName()) {
			f = String.format("DROP CONSTRAINT%s%s", item.getName(), ifNotExistsOrEmpty(context));
		} else {
			switch (item.getType()) {
				case UNIQUE:
					f =  renderUniqueConstraint(item, context);
					break;
				case EXISTS:
					f =  renderPropertyExists(item, context);
					break;
				case KEY:
					f =  renderNodeKey(item, context);
					break;
				default:
					throw new IllegalArgumentException("Unsupported type of constraint: " + item.getType());
			}
		}
		if(version == Version.V3_5 && context.getOperator() == Operator.CREATE) {
			System.out.println(f);
		}
		return f;
	}

	private String renderNodeKey(Constraint item, RenderContext context) {

		if (context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new MigrationsException(
				String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() != TargetEntity.NODE) {
			throw new MigrationsException("Key constraints are only supported for nodes, not for relationships.");
		}

		Name name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);

		if (version == Version.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT %s IS NODE KEY", context.getOperator(), identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("CREATE CONSTRAINT%s ON (n:%s) ASSERT %s IS NODE KEY", name, identifier, properties);
		} else if (Version.RANGE_41_TO_43.contains(version)) {
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
			throw new MigrationsException(
				String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() == TargetEntity.NODE) {
			return renderNodePropertyExists(item, context);
		}
		return renderRelationshipPropertyExists(item, context);
	}

	private String renderRelationshipPropertyExists(Constraint item, RenderContext context) {

		Name name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("r", item);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		if (version == Version.V3_5) {
			return String.format("%s CONSTRAINT ON ()-[r:%s]-() ASSERT exists(%s)", operator, identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("%s CONSTRAINT%s ON ()-[r:%s]-() ASSERT exists(%s)", operator, name, identifier,
				properties);
		} else if (Version.RANGE_41_TO_42.contains(version)) {
			return String.format("%s CONSTRAINT%s %sON ()-[r:%s]-() ASSERT exists(%s)", operator, name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Version.V4_3) {
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

		Name name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);
		Operator operator = context.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		if (version == Version.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT exists(%s)", operator, identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("%s CONSTRAINT%s ON (n:%s) ASSERT exists(%s)", operator, name, identifier, properties);
		} else if (Version.RANGE_41_TO_42.contains(version)) {
			return String.format("%s CONSTRAINT%s %sON (n:%s) ASSERT exists(%s)", operator, name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Version.V4_3) {
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

		Name name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);
		Constraint.Type type = item.getType();
		Operator operator = context.getOperator();

		if (version == Version.V3_5) {
			return String.format("%s CONSTRAINT ON (n:%s) ASSERT %s IS %s", operator, identifier,
				properties, type);
		} else if (version == Version.V4_0) {
			return String.format("%s CONSTRAINT%s ON (n:%s) ASSERT %s IS %s", operator, name, identifier, properties, type);
		} else if (Version.RANGE_41_TO_43.contains(version)) {
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
}
