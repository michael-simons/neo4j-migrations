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
package ac.simons.neo4j.migrations.core.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internally used String utilities. There are no guarantees on the stability of this API.
 * It won't be available when run on the module path.
 *
 * @author Michael J. Simon
 * @since 1.2.2
 */
public final class Strings {

	/**
	 * Single line comment indicator.
	 */
	public static final String CYPHER_SINGLE_LINE_COMMENT = "//";

	/**
	 * Pattern used for splitting lines.
	 */
	public static final String LINE_DELIMITER = "\r?\n|\r";

	/**
	 * Function for creating an MD5 representation of a byte array.
	 */
	public static final UnaryOperator<byte[]> MD5 = bytes -> {
		try {
			@SuppressWarnings("squid:S4790") // Definitely not at risk here.
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return md5.digest(bytes);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new UncheckedNoSuchAlgorithmException(ex);
		}
	};

	/**
	 * A pattern representing valid Neo4j database names as described in <a href=
	 * "https://neo4j.com/docs/cypher-manual/current/databases/#administration-databases-create-database">Database
	 * management</a>.
	 */
	public static final String VALID_DATABASE_NAME = "([a-z][a-z\\d.\\-]{2,62})";

	private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

	/**
	 * A Base64 encoder.
	 */
	public static final Function<byte[], String> BASE64_ENCODING = HEX_FORMAT::formatHex;

	private Strings() {
	}

	/**
	 * Capitalizes a string.
	 * @param value string to capitalize
	 * @return capitalized String or the original value if unchanged or if the value was
	 * {@literal null} or empty.
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
	 * @param value the value to transform
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
			}
			else {
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
	 * This won't match a statement like <pre>
	 *     // Right at the start
	 *     MATCH (n) RETURN count(n) AS n;
	 * </pre> which will pe extracted from a Cypher file as one statement starting with a
	 * comment.
	 * <p>
	 * It would be nice to use the JavaCC Parser here as we do in Cypher-DSL, but that
	 * would require Java 11 for core.
	 * @param statement a statement to check
	 * @return true if the statement is not null and is a single line comment
	 */
	public static boolean isSingleLineComment(String statement) {

		Objects.requireNonNull(statement, "Statement to check must not be null");

		String trimmed = statement.trim();
		if (!trimmed.startsWith(CYPHER_SINGLE_LINE_COMMENT)) {
			return false;
		}

		return !(trimmed.contains("\n") || trimmed.contains("\r"));
	}

	private static boolean valueIsNotBlank(String value) {
		return !value.trim().isEmpty();
	}

	/**
	 * Creates an optional value from a given string value, filtering additionally on
	 * blankness.
	 * @param value the value to create an optional from
	 * @return an optional
	 */
	public static Optional<String> optionalOf(String value) {
		return Optional.ofNullable(value).filter(Strings::valueIsNotBlank).map(String::trim);
	}

	/**
	 * Checks whether a string is blank or not.
	 * @param value the value to check
	 * @return {@literal true} if {@code value} is null or completely blank.
	 */
	public static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Replaces all calls to {@code elementId()} (A Neo4j 5 feature) with a combination of
	 * {@code toString(id())} so that callers can reliably work on String based ids.
	 * @param query the query in which to replace calls to {@code elementId()}
	 * @return an updated query
	 */
	public static String replaceElementIdCalls(String query) {

		String prefix = "toString(id(";
		String postfix = "))";
		StringBuffer sb = new StringBuffer();
		Matcher m = Pattern.compile("([\\s(]?)elementId\\((.+?)\\)").matcher(query);
		while (m.find()) {
			m.appendReplacement(sb, m.group(1) + prefix + m.group(2) + postfix);
		}
		m.appendTail(sb);
		return sb.toString();
	}

}
