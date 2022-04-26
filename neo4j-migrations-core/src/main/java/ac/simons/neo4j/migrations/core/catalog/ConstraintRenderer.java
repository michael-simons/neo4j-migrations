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
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
class ConstraintRenderer implements Renderer<Constraint> {

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

		switch (item.getType()) {
			case UNIQUE:
				return renderUniqueConstraint(item, context);
			case EXISTS:
				return renderPropertyExists(item, context);
			case KEY:
				return renderNodeKey(item, context);
			default:
				throw new IllegalArgumentException("Unsupported type of constraint: " + item.getType());
		}
	}

	private String renderNodeKey(Constraint item, RenderContext context) {

		if (context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new MigrationsException(
				String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() != TargetEntity.NODE) {
			throw new MigrationsException("Key constraints are only supported for nodes, not for relationships.");
		}

		String name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);

		if (version == Version.V3_5) {
			return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT %s IS NODE KEY", identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT %s IS NODE KEY", name, identifier, properties);
		} else if (Version.RANGE_41_TO_43.contains(version)) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT %s IS NODE KEY", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE %s IS NODE KEY", name,
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

		String name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("r", item);

		if (version == Version.V3_5) {
			return String.format("CREATE CONSTRAINT ON ()-[r:%s]-() ASSERT exists(%s)", identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("CREATE CONSTRAINT %s ON ()-[r:%s]-() ASSERT exists(%s)", name, identifier,
				properties);
		} else if (Version.RANGE_41_TO_42.contains(version)) {
			return String.format("CREATE CONSTRAINT %s %sON ()-[r:%s]-() ASSERT exists(%s)", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Version.V4_3) {
			return String.format("CREATE CONSTRAINT %s %sON ()-[r:%s]-() ASSERT %s IS NOT NULL", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR ()-[r:%s]-() REQUIRE %s IS NOT NULL", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		}
	}

	private String renderNodePropertyExists(Constraint item, RenderContext context) {

		String name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);

		if (version == Version.V3_5) {
			return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT exists(%s)", identifier, properties);
		} else if (version == Version.V4_0) {
			return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT exists(%s)", name, identifier, properties);
		} else if (Version.RANGE_41_TO_42.contains(version)) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT exists(%s)", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else if (version == Version.V4_3) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT %s IS NOT NULL", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE %s IS NOT NULL", name,
				ifNotExistsOrEmpty(context), identifier, properties);
		}
	}

	private static String ifNotExistsOrEmpty(RenderContext context) {
		return context.isIdempotent() ? "IF NOT EXISTS " : "";
	}

	private String renderUniqueConstraint(Constraint item, RenderContext context) {

		String name = item.getName();
		Version version = context.getVersion();
		String identifier = item.getIdentifier();
		String properties = renderProperties("n", item);
		Constraint.Type type = item.getType();

		if (version == Version.V3_5) {
			return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT %s IS %s", identifier,
				properties, type);
		} else if (version == Version.V4_0) {
			return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT %s IS %s", name, identifier, properties, type);
		} else if (Version.RANGE_41_TO_43.contains(version)) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT %s IS %s", name, ifNotExistsOrEmpty(context),
				identifier, properties, type);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE %s IS %s", name,
				ifNotExistsOrEmpty(context), identifier, properties, type);
		}
	}

	private static String renderProperties(String prefix, Constraint item) {

		if (item.getProperties().size() == 1) {
			return prefix + "." + item.getProperties().iterator().next();
		}

		return item.getProperties().stream().map(v -> prefix + "." + v).collect(Collectors.joining(", ", "(", ")"));
	}
}
