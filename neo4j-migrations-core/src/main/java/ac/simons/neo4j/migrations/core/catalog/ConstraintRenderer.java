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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
class ConstraintRenderer implements Renderer<Constraint> {

	private static final Set<String> RANGE_41_TO_43 = new LinkedHashSet<>(Arrays.asList("4.1", "4.2", "4.3"));
	private static final Set<String> RANGE_41_TO_42 = new LinkedHashSet<>(Arrays.asList("4.1", "4.2"));

	@Override
	public String render(Constraint item, RenderContext context) {

		switch (item.getType()) {
			case UNIQUE:
				return renderUniqueConstraint(item, context);
			case EXISTS:
				return renderPropertyExists(item, context);
			default:
				throw new IllegalArgumentException("Unsupported type of constraint: " + item.getType());
		}
	}

	private String renderPropertyExists(Constraint item, RenderContext context) {

		if (context.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new MigrationsException(String.format("This constraint cannot be be used with %s edition.", context.getEdition()));
		}

		if (item.getTarget() == Constraint.Target.NODE) {
			return renderNodePropertyExists(item, context);
		}
		return renderRelationshipPropertyExists(item, context);
	}

	private String renderRelationshipPropertyExists(Constraint item, RenderContext context) {

		String name = item.getName();
		String version = context.getVersion();
		String identifier = item.getIdentifier();
		String requiredSingleProperty = item.getRequiredSingleProperty();

		if (version.startsWith("3.5")) {
			return String.format("CREATE CONSTRAINT ON ()-[r:%s]-() ASSERT exists(r.%s)", identifier, requiredSingleProperty);
		} else if (version.startsWith("4.0")) {
			return String.format("CREATE CONSTRAINT %s ON ()-[r:%s]-() ASSERT exists(r.%s)", name, identifier, requiredSingleProperty);
		} else if (RANGE_41_TO_42.stream().anyMatch(version::startsWith)) {
			return String.format("CREATE CONSTRAINT %s %sON ()-[r:%s]-() ASSERT exists(r.%s)", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		} else if (version.startsWith("4.3")) {
			return String.format("CREATE CONSTRAINT %s %sON ()-[r:%s]-() ASSERT r.%s IS NOT NULL", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR ()-[r:%s]-() REQUIRE r.%s IS NOT NULL", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		}
	}

	private String renderNodePropertyExists(Constraint item, RenderContext context) {

		String name = item.getName();
		String version = context.getVersion();
		String identifier = item.getIdentifier();
		String requiredSingleProperty = item.getRequiredSingleProperty();

		if (version.startsWith("3.5")) {
			return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT exists(n.%s)", identifier, requiredSingleProperty);
		} else if (version.startsWith("4.0")) {
			return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT exists(n.%s)", name, identifier, requiredSingleProperty);
		} else if (RANGE_41_TO_42.stream().anyMatch(version::startsWith)) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT exists(n.%s)", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		} else if (version.startsWith("4.3")) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT n.%s IS NOT NULL", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE n.%s IS NOT NULL", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty);
		}
	}

	private static String ifNotExistsOrEmpty(RenderContext context) {
		return context.isIdempotent() ? "IF NOT EXISTS " : "";
	}

	private String renderUniqueConstraint(Constraint item, RenderContext context) {

		String name = item.getName();
		String version = context.getVersion();
		String identifier = item.getIdentifier();
		String requiredSingleProperty = item.getRequiredSingleProperty();
		Constraint.Kind type = item.getType();

		if (version.startsWith("3.5")) {
			return String.format("CREATE CONSTRAINT ON (n:%s) ASSERT n.%s IS %s", identifier,
					requiredSingleProperty, type);
		} else if (version.startsWith("4.0")) {
			return String.format("CREATE CONSTRAINT %s ON (n:%s) ASSERT n.%s IS %s", name, identifier, requiredSingleProperty, type);
		} else if (RANGE_41_TO_43.stream().anyMatch(version::startsWith)) {
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT n.%s IS %s", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty, type);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE n.%s IS %s", name, ifNotExistsOrEmpty(context), identifier, requiredSingleProperty, type);
		}
	}
}
