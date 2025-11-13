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

import java.util.Objects;

import ac.simons.neo4j.migrations.core.refactorings.DefaultRename.Target;

/**
 * Renames entities or properties of entities.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
public sealed interface Rename extends CustomizableRefactoring<Rename> permits DefaultRename {

	/**
	 * Provides a refactoring renaming a given node label. Any customization can be done
	 * directly on the {@link Rename} instance.
	 * @param from the label to rename
	 * @param to the new label
	 * @return the refactoring ready to use
	 */
	static Rename label(String from, String to) {
		return new DefaultRename(Target.LABEL, Objects.requireNonNull(from), Objects.requireNonNull(to));
	}

	/**
	 * Provides a refactoring renaming a given relationship type. Any customization can be
	 * done directly on the {@link Rename} instance.
	 * @param from the type to rename
	 * @param to the new type
	 * @return the refactoring ready to use
	 */
	static Rename type(String from, String to) {
		return new DefaultRename(Target.TYPE, Objects.requireNonNull(from), Objects.requireNonNull(to));
	}

	/**
	 * Provides a refactoring renaming a property on a node. Any customization can be done
	 * directly on the {@link Rename} instance. If the property does not exist on the
	 * given node, nothing changes.
	 * @param from the property to rename
	 * @param to the new name of the property
	 * @return the refactoring ready to use
	 */
	static Rename nodeProperty(String from, String to) {
		return new DefaultRename(Target.NODE_PROPERTY, Objects.requireNonNull(from), Objects.requireNonNull(to));
	}

	/**
	 * Provides a refactoring renaming a property on a relationship. Any customization can
	 * be done directly on the {@link Rename} instance. If the property does not exist on
	 * the given relationship, nothing changes.
	 * @param from the property to rename
	 * @param to the new name of the property
	 * @return the refactoring ready to use
	 */
	static Rename relationshipProperty(String from, String to) {
		return new DefaultRename(Target.REL_PROPERTY, Objects.requireNonNull(from), Objects.requireNonNull(to));
	}

}
