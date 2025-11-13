/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.Optional;

/**
 * A specialization of the {@link OperationResult} that always affects a database, either
 * the default one or a named other.
 *
 * @author Michael J. Simons
 * @since 1.2.0
 */
public sealed interface DatabaseOperationResult extends OperationResult
		permits CleanResult, ValidationResult, AbstractRepairmentResult {

	/**
	 * Returns the optional name of the database clean, an empty optional indicates the
	 * default database.
	 * @return the optional name of the database clean
	 */
	Optional<String> getAffectedDatabase();

}
