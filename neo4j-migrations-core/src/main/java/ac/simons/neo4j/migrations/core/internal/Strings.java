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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internally used String utilities. There are no guarantees on the stability of this API. It won't be available when
 * run on the module path.
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
		} catch (NoSuchAlgorithmException e) {
			throw new UncheckedNoSuchAlgorithmException(e);
		}
	};

	/**
	 * A pattern representing valid Neo4j database names as described in
	 * <a href="https://neo4j.com/docs/cypher-manual/current/databases/#administration-databases-create-database">Database management</a>.
	 */
	public static final String VALID_DATABASE_NAME = "([a-z][a-z\\d.\\-]{2,62})";

	private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

	private static final String BACKTICK_OR_UC = "[`\\\\\u0060]";

	private static final Pattern LABEL_AND_TYPE_QUOTATION = Pattern.compile(
		String.format("(?<!%1$s)%1$s(?:%1$s{2})*(?!%1$s)", BACKTICK_OR_UC));

	/**
	 * A Base64 encoder.
	 */
	public static final Function<byte[], String> BASE64_ENCODING = bytes -> {
		final StringBuilder sb = new StringBuilder(2 * bytes.length);
		for (byte b : bytes) {
			sb.append(HEX_DIGITS[(b >> 4) & 0xf]).append(HEX_DIGITS[b & 0xf]);
		}
		return sb.toString();
	};

	/**
	 * Capitalizes a string
	 *
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

	private static boolean valueIsNotBlank(String value) {
		return !value.trim().isEmpty();
	}

	/**
	 * Creates an optional value from a given string value, filtering additionally on blankness.
	 *
	 * @param value The value to create an optional from
	 * @return An optional
	 */
	public static Optional<String> optionalOf(String value) {
		return Optional.ofNullable(value)
			.filter(Strings::valueIsNotBlank)
			.map(String::trim);
	}

	/**
	 * @param value The value to check
	 * @return {@literal true} if {@code value} is null or completely blank.
	 */
	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * This is a literal copy of {@code javax.lang.model.SourceVersion#isIdentifier(CharSequence)} included here to
	 * be not dependent on the compiler module.
	 *
	 * @param name A possible Java identifier
	 * @return True, if {@code name} represents an identifier.
	 */
	public static boolean isIdentifier(CharSequence name) {
		String id = name.toString();

		if (id.length() == 0) {
			return false;
		}
		int cp = id.codePointAt(0);
		if (!Character.isJavaIdentifierStart(cp)) {
			return false;
		}
		for (int i = Character.charCount(cp); i < id.length(); i += Character.charCount(cp)) {
			cp = id.codePointAt(i);
			if (!Character.isJavaIdentifierPart(cp)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Escapes the string {@literal potentiallyNonIdentifier} in all cases when it's not a valid Cypher identifier.
	 *
	 * @param potentiallyNonIdentifier A value to escape
	 * @return The escaped value or the same value if no escaping is necessary.
	 */
	public static String escapeIfNecessary(String potentiallyNonIdentifier) {

		if (potentiallyNonIdentifier == null || potentiallyNonIdentifier.trim().isEmpty() || Strings.isIdentifier(
			potentiallyNonIdentifier)) {
			return potentiallyNonIdentifier;
		}

		Matcher matcher = LABEL_AND_TYPE_QUOTATION.matcher(potentiallyNonIdentifier);
		return String.format(Locale.ENGLISH, "`%s`", matcher.replaceAll("`$0"));
	}

	/**
	 * Replaces all calls to {@code elementId()} (A Neo4j 5 feature) with a combination of {@code toString(id())} so that
	 * callers can reliably work on String based ids.
	 *
	 * @param query The query in which to replace calls to {@code elementId()}
	 * @return An updated query
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

	private Strings() {
	}
}
