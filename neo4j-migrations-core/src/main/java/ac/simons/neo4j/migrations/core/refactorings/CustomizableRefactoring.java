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
package ac.simons.neo4j.migrations.core.refactorings;

/**
 * A customizable refactoring. Customizations here include providing a custom query as
 * source for entities and a batch size.
 *
 * @param <T> the type of the refactoring after customization has been applied
 * @author Michael J. Simons
 * @since 1.10.0
 */
public interface CustomizableRefactoring<T extends CustomizableRefactoring<T>> extends Refactoring {

	/**
	 * Creates a new {@link Rename refactoring} that may use batching (if the new batch
	 * size is not null and greater than 1.
	 * @param newBatchSize use {@literal null} to disable batching or any value >= 1 to
	 * use batches.
	 * @return a new refactoring.
	 */
	T inBatchesOf(Integer newBatchSize);

	/**
	 * Creates a new {@link Rename refactoring} that may use a custom query.
	 * @param newCustomQuery use {@literal null} to disable any custom query or a valid
	 * Cypher statement returning a single entity column to enable custom query
	 * @return a new refactoring.
	 */
	T withCustomQuery(String newCustomQuery);

}
