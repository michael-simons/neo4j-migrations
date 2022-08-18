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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Normalizes Graph properties
 *
 * @author Michael J. Simons
 * @soundtrack Black Sabbath - Black Sabbath
 * @since 1.10.0
 */
public interface Normalize extends CustomizableRefactoring<Normalize> {

	/**
	 * Normalizes respectively converts the property to a boolean or deletes it in case it is not in the lists of true or
	 * false values.
	 * <p>
	 * If {@code trueValues} or {@code falseValues} contains the literal value {@literal null} than a property with either
	 * the value {@literal true} or {@literal false} will be added to all matched entities.
	 *
	 * @param property    The name of the property to normalize. All entities that spot such a property will be matched
	 *                    (nodes and relationships)
	 * @param trueValues  A list of values that should be treated as {@literal true}. Can be a mixed content list.
	 * @param falseValues A list of values that should be treated as {@literal false}. Can be a mixed content list.
	 * @return The refactoring ready to use
	 */
	static Normalize asBoolean(String property, List<Object> trueValues, List<Object> falseValues) {

		String pn = Objects.requireNonNull(property);
		List<Object> tv = trueValues == null ? Collections.emptyList() : new ArrayList<>(trueValues);
		List<Object> fv = falseValues == null ? Collections.emptyList() : new ArrayList<>(falseValues);

		return new DefaultNormalize(pn, tv, fv);
	}
}
