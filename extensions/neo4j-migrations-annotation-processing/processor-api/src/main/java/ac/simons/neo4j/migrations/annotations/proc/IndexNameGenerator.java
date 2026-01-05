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
package ac.simons.neo4j.migrations.annotations.proc;

import java.util.Collection;

import ac.simons.neo4j.migrations.core.catalog.Index;

/**
 * Generator for index names.
 *
 * @author Michael J. Simons
 * @since 1.11.0
 */
@FunctionalInterface
public interface IndexNameGenerator {

	/**
	 * Generates a name for an index with the given
	 * {@link ac.simons.neo4j.migrations.core.catalog.Index.Type type} for the given list
	 * of {@link PropertyType properties}.
	 * @param type the type of the index
	 * @param properties the properties to create the index for. All properties will have
	 * the same owner.
	 * @return a valid index name
	 */
	String generateName(Index.Type type, Collection<PropertyType<?>> properties);

}
