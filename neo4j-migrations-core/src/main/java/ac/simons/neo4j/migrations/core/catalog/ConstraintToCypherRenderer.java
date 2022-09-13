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

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Formattable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders constraints (supported operators are {@link Operator#CREATE} and {@link Operator#DROP}) as Cypher.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since 1.7.0
 */
enum ConstraintToCypherRenderer implements Renderer<Constraint> {

	INSTANCE;

	/**
	 * A range of versions from 4.1 to 4.3.
	 */
	private static final Set<Neo4jVersion> RANGE_41_TO_43 = EnumSet.of(Neo4jVersion.V4_1, Neo4jVersion.V4_2, Neo4jVersion.V4_3);
	/**
	 * A range of versions from 4.1 to 4.2.
	 */
	private static final Set<Neo4jVersion> RANGE_41_TO_42 = EnumSet.of(Neo4jVersion.V4_1, Neo4jVersion.V4_2);
	private static final String KEYWORD_REQUIRE = "REQUIRE";
	private static final String KEYWORD_ASSERT = "ASSERT";

	@Override
	public void render(Constraint constraint, RenderConfig config, OutputStream target) throws IOException {

		Neo4jVersion version = config.getVersion();
		if (config.isIdempotent() && !version.hasIdempotentOperations() && !config.isIgnoreName()) {
			throw new IllegalArgumentException(
				String.format("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.",
					version));
		}

		if (constraint.getProperties().size() > 1) {
			if (!EnumSet.of(Constraint.Type.UNIQUE, Constraint.Type.KEY).contains(constraint.getType())) {
				throw new IllegalArgumentException("Only unique and node key constraints support multiple properties.");
			}

			if (config.isVersionPriorTo44() && constraint.getType() != Constraint.Type.KEY) {
				throw new IllegalArgumentException("Constraints require exactly one property prior to Neo4j 4.4.");
			}
		}

		if (!constraint.hasName() && config.isIdempotent() && config.getOperator() == Operator.DROP) {
			throw new IllegalArgumentException("The constraint can only be rendered in the given context when having a name.");
		}

		Writer w = new BufferedWriter(new OutputStreamWriter(target, StandardCharsets.UTF_8));
		if (config.getOperator() == Operator.DROP && config.getVersion() != Neo4jVersion.V3_5 && constraint.hasName() && !config.isIgnoreName()) {
			w.write(String.format("DROP %#s%s", constraint, config.ifNotExistsOrEmpty()));
		} else {
			switch (constraint.getType()) {
				case UNIQUE:
					w.write(renderUniqueConstraint(constraint, config));
					break;
				case EXISTS:
					w.write(renderPropertyExists(constraint, config));
					break;
				case KEY:
					w.write(renderNodeKey(constraint, config));
					break;
				default:
					throw new IllegalArgumentException("Unsupported type of constraint: " + constraint.getType());
			}
		}
		w.flush();
	}

	private String renderNodeKey(Constraint constraint, RenderConfig config) {

		if (config.getOperator() == Operator.CREATE && config.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be created with %s edition.", config.getEdition()));
		}

		Neo4jVersion version = config.getVersion();

		Formattable item = formattableItem(constraint, config);
		String identifier = version.sanitizeSchemaName(constraint.getIdentifier());
		String properties = renderProperties(version, "n", constraint);
		Operator operator = config.getOperator();

		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON (n:%s) ASSERT %s IS NODE KEY", operator, item, identifier, properties);
		} else if (RANGE_41_TO_43.contains(version)) {
			return String.format("%s %#s %sON (n:%s) ASSERT %s IS NODE KEY", operator, item,
				config.ifNotExistsOrEmpty(), identifier, properties);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? KEYWORD_REQUIRE : KEYWORD_ASSERT;
			// We just assume the newest
			return String.format("%s %#s %s%s (n:%s) %s %s IS NODE KEY", operator, item,
				config.ifNotExistsOrEmpty(), adjective, identifier, verb, properties);
		}
	}

	private String renderPropertyExists(Constraint item, RenderConfig config) {

		if (config.getOperator() == Operator.CREATE && config.getEdition() != Neo4jEdition.ENTERPRISE) {
			throw new IllegalStateException(
				String.format("This constraint cannot be created with %s edition.", config.getEdition()));
		}

		if (item.getTargetEntityType() == TargetEntityType.NODE) {
			return renderNodePropertyExists(item, config);
		}
		return renderRelationshipPropertyExists(item, config);
	}

	private String renderRelationshipPropertyExists(Constraint constraint, RenderConfig config) {

		return renderPropertyExists(constraint, config, "r", "()-[%s:%s]-()");
	}

	private String renderNodePropertyExists(Constraint constraint, RenderConfig config) {

		return renderPropertyExists(constraint, config, "n", "(%s:%s)");
	}

	private String renderPropertyExists(Constraint constraint, RenderConfig config, String variable, String templateFragment) {

		Neo4jVersion version = config.getVersion();

		Formattable item = formattableItem(constraint, config);
		String identifier = version.sanitizeSchemaName(constraint.getIdentifier());
		String properties = renderProperties(version, variable, constraint);
		Operator operator = config.getOperator();
		String object = String.format(operator == Operator.CREATE ? "%s IS NOT NULL" : "exists(%s)", properties);

		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			String format = "%s %#s ON " + templateFragment + " ASSERT exists(%s)";
			return String.format(format, operator, item, variable, identifier, properties);
		} else if (RANGE_41_TO_42.contains(version)) {
			String format = "%s %#s %sON " + templateFragment + " ASSERT exists(%s)";
			return String.format(format, operator, item, config.ifNotExistsOrEmpty(), variable, identifier, properties);
		} else if (version == Neo4jVersion.V4_3) {
			String format = "%s %#s %sON " + templateFragment + " ASSERT %s";
			return String.format(format, operator, item, config.ifNotExistsOrEmpty(), variable, identifier, object);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? KEYWORD_REQUIRE : KEYWORD_ASSERT;
			// We just assume the newest
			String format = "%s %#s %s%s " + templateFragment + " %s %s";
			return String.format(format, operator, item, config.ifNotExistsOrEmpty(), adjective, variable, identifier, verb, object);
		}
	}

	private String renderUniqueConstraint(Constraint constraint, RenderConfig config) {

		if (constraint.getProperties().size() > 1 && config.getVersion().isPriorTo44()) {
			throw new IllegalArgumentException("Composite unique constraints are not supported prior to Neo4j/4.4.");
		}

		Neo4jVersion version = config.getVersion();

		Formattable item = formattableItem(constraint, config);
		String identifier = version.sanitizeSchemaName(constraint.getIdentifier());
		String properties = renderProperties(version, "n", constraint);
		Constraint.Type type = constraint.getType();
		Operator operator = config.getOperator();

		if (version == Neo4jVersion.V3_5 || version == Neo4jVersion.V4_0) {
			return String.format("%s %#s ON (n:%s) ASSERT %s IS %s", operator, item, identifier, properties, type);
		} else if (RANGE_41_TO_43.contains(version)) {
			return String.format("%s %#s %sON (n:%s) ASSERT %s IS %s", operator, item, config.ifNotExistsOrEmpty(),
				identifier, properties, type);
		} else {
			String adjective = operator == Operator.CREATE ? "FOR" : "ON";
			String verb = operator == Operator.CREATE ? KEYWORD_REQUIRE : KEYWORD_ASSERT;
			// We just assume the newest
			return String.format("%s %#s %s%s (n:%s) %s %s IS %s", operator, item,
				config.ifNotExistsOrEmpty(), adjective, identifier, verb, properties, type);
		}
	}

	private static String renderProperties(Neo4jVersion version, String prefix, Constraint item) {

		if (item.getProperties().size() == 1) {
			return prefix + "." + version.sanitizeSchemaName(item.getProperties().iterator().next());
		}

		return item.getProperties().stream()
			.map(version::sanitizeSchemaName)
			.map(v -> prefix + "." + v).collect(Collectors.joining(", ", "(", ")"));
	}

	Formattable formattableItem(Constraint item, RenderConfig config) {
		return config.isIgnoreName() || config.getVersion() == Neo4jVersion.V3_5 ? new AnonymousCatalogItem(item) : item;
	}
}
