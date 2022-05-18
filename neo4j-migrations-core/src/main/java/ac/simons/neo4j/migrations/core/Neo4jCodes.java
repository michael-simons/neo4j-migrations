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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of internal Neo4j codes we rely on in several occasions.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Die Reaktion - The Last Jazz, Vol. II
 * @since TBA
 */
final class Neo4jCodes {

	static final Set<String> CODES_FOR_EXISTING_CONSTRAINT = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList("Neo.ClientError.Schema.EquivalentSchemaRuleAlreadyExists",
			"Neo.ClientError.Schema.ConstraintAlreadyExists")));

	static final String CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE = "Neo.ClientError.Schema.ConstraintWithNameAlreadyExists";

	static final String CONSTRAINT_DROP_FAILED = "Neo.DatabaseError.Schema.ConstraintDropFailed";

	private Neo4jCodes() {
	}
}
