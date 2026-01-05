/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;

/**
 * Utility mixin.
 *
 * @author Michael J. Simons
 */
interface AbstractNameGeneratorForCatalogItems {

	/**
	 * Generate a name for this item.
	 * @param type the type of the item
	 * @param properties the items properties
	 * @return a generated, stable name
	 */
	default String generateName(String type, Collection<PropertyType<?>> properties) {

		ElementType<?> owner = properties.stream()
			.findFirst()
			.map(PropertyType::getOwner)
			.orElseThrow(
					() -> new IllegalArgumentException("Empty collection of properties passed to the name generator"));

		String propertyNames = properties.stream().map(PropertyType::getName).collect(Collectors.joining("_"));

		// Basically the OGM approach
		return String.format("%s_%s_%s", owner.getOwningTypeName().toLowerCase(Locale.ROOT).replace(".", "_"),
				propertyNames, type.toLowerCase(Locale.ROOT));
	}

}
