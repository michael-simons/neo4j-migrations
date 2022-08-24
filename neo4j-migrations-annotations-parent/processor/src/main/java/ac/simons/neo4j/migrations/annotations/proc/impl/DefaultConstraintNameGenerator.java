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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Verliebt
 * @since TBA
 */
final class DefaultConstraintNameGenerator implements ConstraintNameGenerator {

	@Override
	public String generateName(Constraint.Type constraintType, Collection<PropertyType<NodeType>> properties) {


		NodeType owner = properties.stream()
			.findFirst().map(PropertyType::getOwner)
			.orElseThrow(() -> new IllegalArgumentException("Empty collection of properties passed to the name generator"));

		String propertyNames = properties.stream()
			.map(PropertyType::getName).collect(Collectors.joining("_"));

		// Basically the OGM approach
		return String.format("%s_%s_%s",
			owner.getOwningTypeName().toLowerCase(Locale.ROOT).replace(".", "_"),
			propertyNames, constraintType.name().toLowerCase(Locale.ROOT));
	}
}