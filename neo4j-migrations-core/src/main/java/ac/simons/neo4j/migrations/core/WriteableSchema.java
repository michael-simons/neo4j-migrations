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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.schema.Constraint;
import ac.simons.neo4j.migrations.core.schema.Schema;

import java.util.List;

/**
 * A marker interface that can be added to a {@link ac.simons.neo4j.migrations.core.schema.Schema} making it writable.
 *
 * @author Michael J. Simons
 * @since TBA
 */
interface WriteableSchema extends Schema {

	void addAll(MigrationVersion version, List<Constraint> newConstraints);
}
