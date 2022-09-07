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

import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;

/**
 * Helper for creating properties on element types
 *
 * @author Michael J. Simons
 * @param <ET> The concrete element type
 * @since 1.11.0
 */
@SuppressWarnings("squid:S119") // I like the generic parameter name.
interface WriteableElementType<ET extends ElementType<ET>> {

	/**
	 * Adds the given property
	 *
	 * @param property The property to add
	 * @return The new property created, with {@literal this} as the owner
	 */
	PropertyType<ET> addProperty(String property);
}
