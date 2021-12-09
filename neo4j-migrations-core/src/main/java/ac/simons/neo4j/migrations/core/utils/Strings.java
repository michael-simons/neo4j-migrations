/*
 * Copyright 2020-2021 the original author or authors.
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
package ac.simons.neo4j.migrations.core.utils;

import java.util.function.UnaryOperator;

/**
 * Internally used String utilities. There are no guarantees on the stability of this API. It won't be available when
 * run on the module path.
 *
 * @author Michael J. Simon
 * @since TBA
 */
public final class Strings {

	/**
	 * Transforms a string with words separated by {@literal _} into a camelCase string.
	 *
	 * @param value The value to transform
	 * @return the value in camelCase
	 */
	public static String toCamelCase(String value) {
		StringBuilder sb = new StringBuilder();

		int i = 0;
		final int u = '_';
		int codePoint;
		Integer prev = null;
		boolean nextUpper = false;
		while (i < value.length()) {
			codePoint = value.codePointAt(i);
			i += Character.charCount(codePoint);
			if (codePoint == u) {
				nextUpper = true;
			} else {
				UnaryOperator<Integer> transform = Character::toLowerCase;
				if (nextUpper || (prev != null && Character.isLowerCase(prev) && Character.isUpperCase(codePoint))) {
					transform = Character::toUpperCase;
				}
				prev = codePoint;
				sb.append(Character.toChars(transform.apply(codePoint)));
				nextUpper = false;
			}
		}

		return sb.toString();
	}

	private Strings() {
	}
}
