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
package ac.simons.neo4j.migrations.core;

import java.util.Set;

/**
 * A set of internal Neo4j codes we rely on in several occasions.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class Neo4jCodes {

	static final String CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE = "Neo.ClientError.Schema.ConstraintWithNameAlreadyExists";

	static final String CONSTRAINT_DROP_FAILED = "Neo.DatabaseError.Schema.ConstraintDropFailed";

	/**
	 * Used in [3.5, 4.4] when a constraint without a name is defined twice and the 2nd
	 * one is equivalent.
	 */
	static final String EQUIVALENT_SCHEMA_RULE_ALREADY_EXISTS = "Neo.ClientError.Schema.EquivalentSchemaRuleAlreadyExists";

	/**
	 * Used in [4,4, x] when a constraint with a name is semantically equivalent to a
	 * constraint defined without a name.
	 */
	static final String CONSTRAINT_ALREADY_EXISTS = "Neo.ClientError.Schema.ConstraintAlreadyExists";

	/**
	 * Used in notifications to warn about deprecated features.
	 */
	static final String FEATURE_DEPRECATION_WARNING = "Neo.ClientNotification.Statement.FeatureDeprecationWarning";

	/**
	 * Used when a procedure was not found.
	 */
	static final String PROCEDURE_NOT_FOUND = "Neo.ClientError.Procedure.ProcedureNotFound";

	static final Set<String> CODES_FOR_EXISTING_CONSTRAINT = Set.of(EQUIVALENT_SCHEMA_RULE_ALREADY_EXISTS,
			CONSTRAINT_ALREADY_EXISTS);

	/**
	 * Used when the creation of a constraint fails.
	 */
	static final String CONSTRAINT_CREATION_FAILED = "Neo.DatabaseError.Schema.ConstraintCreationFailed";

	private Neo4jCodes() {
	}

}
