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
package ac.simons.neo4j.migrations.core.refactorings;

import ac.simons.neo4j.migrations.core.refactorings.DefaultRename.Target;

/**
 * Renames entities or properties of entities.
 * @author Michael J. Simons
 * @since 1.10.0
 */
public interface Rename extends Refactoring {

	/**
	 * Provides a refactoring renaming a given node label. Any customization can be done directly on the {@link Rename} instance.
	 *
	 * @param from The label to rename
	 * @param to   The new label
	 * @return The refactoring ready to use
	 */
	static Rename label(String from, String to) {
		return new DefaultRename(Target.LABEL, from, to);
	}

	/**
	 * Provides a refactoring renaming a given relationship type. Any customization can be done directly on the {@link Rename} instance.
	 *
	 * @param from The type to rename
	 * @param to   The new type
	 * @return The refactoring ready to use
	 */
	static Rename type(String from, String to) {
		return new DefaultRename(Target.TYPE, from, to);
	}

	/**
	 * Provides a refactoring renaming a property on a node. Any customization can be done directly on the {@link Rename} instance.
	 * If the property does not exist on the given node, nothing changes.
	 *
	 * @param from The property to rename
	 * @param to   The new name of the property
	 * @return The refactoring ready to use
	 */
	static Rename nodeProperty(String from, String to) {
		return new DefaultRename(Target.NODE_PROPERTY, from, to);
	}

	/**
	 * Provides a refactoring renaming a property on a relationship. Any customization can be done directly on the {@link Rename} instance.
	 * If the property does not exist on the given relationship, nothing changes.
	 *
	 * @param from The property to rename
	 * @param to   The new name of the property
	 * @return The refactoring ready to use
	 */
	static Rename relationshipProperty(String from, String to) {
		return new DefaultRename(Target.REL_PROPERTY, from, to);
	}

	/**
	 * Creates a new {@link Rename refactoring} that may use batching (if the new batch size is not null and greater than 1
	 *
	 * @param newBatchSize Use {@literal null} to disable batching or any value >= 1 to use batches.
	 * @return A new refactoring.
	 */
	Rename inBatchesOf(Integer newBatchSize);

	/**
	 * Creates a new {@link Rename refactoring} that may use a custom query
	 *
	 * @param newCustomQuery Use {@literal null} to disable any custom query or a valid Cypher statement returning a single
	 *                       entity column to enable custom query
	 * @return A new refactoring.
	 */
	Rename withCustomQuery(String newCustomQuery);
}
