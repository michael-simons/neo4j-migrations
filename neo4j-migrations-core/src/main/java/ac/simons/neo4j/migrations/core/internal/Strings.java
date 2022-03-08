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
package ac.simons.neo4j.migrations.core.internal;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Internally used String utilities. There are no guarantees on the stability of this API. It won't be available when
 * run on the module path.
 *
 * @author Michael J. Simon
 * @since 1.2.2
 */
public final class Strings {

	/**
	 * Single line comment indicator
	 */
	public static final String CYPHER_SINGLE_LINE_COMMENT = "//";

	/**
	 * Capitalizees a string
	 * @param value String to capitalize
	 * @return Capitalized String or the original value if unchanged or if the value was {@literal null} or empty.
	 */
	public static String capitalize(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}

		char baseChar = value.charAt(0);
		char updatedChar = Character.toUpperCase(baseChar);
		if (baseChar == updatedChar) {
			return value;
		}

		char[] chars = value.toCharArray();
		chars[0] = updatedChar;
		return new String(chars, 0, chars.length);
	}

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

	/**
	 * This won't match a statement like
	 * <pre>
	 *     // Right at the start
	 *     MATCH (n) RETURN count(n) AS n;
	 * </pre>
	 * which will pe extracted from a Cypher file as one statement starting with a comment.
	 * <p>
	 * It would be nice to use the JavaCC Parser here as we do in Cypher-DSL, but that would require Java 11 for core.
	 *
	 * @param statement A statement to check
	 * @return True if the statement is not null and is a single line comment
	 */
	public static boolean isSingleLineComment(String statement) {

		Objects.requireNonNull(statement, "Statement to check must not be null");

		String trimmed = statement.trim();
		if (!trimmed.startsWith(CYPHER_SINGLE_LINE_COMMENT)) {
			return false;
		}

		return !(trimmed.contains("\n") || trimmed.contains("\r"));
	}

	private Strings() {
	}
}
