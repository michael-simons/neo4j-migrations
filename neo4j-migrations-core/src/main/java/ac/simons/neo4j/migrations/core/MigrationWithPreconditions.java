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

import java.util.Collections;
import java.util.List;

/**
 * Internal marker interface for migrations allowing preconditions. In case preconditions
 * change the checksum of a migration, alternative checksums can be recognized for a given
 * migration as well.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
non-sealed interface MigrationWithPreconditions extends Migration {

	/**
	 * {@return a list of preconditions}
	 */
	List<Precondition> getPreconditions();

	/**
	 * Returns potentially alternative checksums caused by the content being different due
	 * to the way the specific resource handles preconditions.
	 * @return a list of alternative checksums
	 */
	default List<String> getAlternativeChecksums() {
		return Collections.emptyList();
	}

	/**
	 * Defaults to a no-op.
	 * @param alternativeChecksums a list of alternative checksums
	 */
	default void setAlternativeChecksums(List<String> alternativeChecksums) {
	}

}
