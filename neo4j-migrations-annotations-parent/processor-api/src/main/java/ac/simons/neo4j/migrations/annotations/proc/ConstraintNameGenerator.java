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
package ac.simons.neo4j.migrations.annotations.proc;

import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.util.Collection;

/**
 * Contract for generating names for primary keys representing the unique identifier of a concrete model class.
 *
 * @author Michael J. Simons
 * @soundtrack Moonbootica - ...And Then We Started To Dance
 * @since TBA
 */
@FunctionalInterface
public interface ConstraintNameGenerator {

	/**
	 * Generate a primary key name for the given combination of a {@link  NodeType node-} and a {@link PropertyType property type}.
	 *
	 * @param properties The properties to create the constraint for. All properties will have the same owner.
	 * @return A valid primary key (constraint) name
	 */
	String generateName(Constraint.Type constraintType, Collection<PropertyType<NodeType>> properties);
}
