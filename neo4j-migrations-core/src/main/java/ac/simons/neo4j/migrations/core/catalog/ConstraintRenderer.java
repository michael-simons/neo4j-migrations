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

	@Override
	public String render(Constraint item, RenderContext context) {

		if (item.getType() == Constraint.Kind.UNIQUE) {
			return renderUniqueConstraint(item, context);
		}
		throw new IllegalArgumentException("Unsupported type of constraint: " + item.getType());
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
			return String.format("CREATE CONSTRAINT %s %sON (n:%s) ASSERT n.%s IS %s", name, context.isIdempotent() ? "IF NOT EXISTS " : "", identifier, requiredSingleProperty, type);
		} else {
			// We just assume the newest
			return String.format("CREATE CONSTRAINT %s %sFOR (n:%s) REQUIRE n.%s IS %s", name, context.isIdempotent() ? "IF NOT EXISTS " : "", identifier, requiredSingleProperty, type);
		}
	}
}
