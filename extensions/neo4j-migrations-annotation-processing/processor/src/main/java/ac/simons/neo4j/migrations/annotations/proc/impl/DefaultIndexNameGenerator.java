/*
 * Copyright 2020-2023 the original author or authors.
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

import ac.simons.neo4j.migrations.annotations.proc.IndexNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Index;

import java.util.Collection;

/**
 * @author Michael J. Simons
 * @since 1.11.0
 */
public class DefaultIndexNameGenerator implements IndexNameGenerator, AbstractNameGeneratorForCatalogItems {

	@Override
	public String generateName(Index.Type type, Collection<PropertyType<?>> properties) {

		return generateName(type.name(), properties);
	}
}
