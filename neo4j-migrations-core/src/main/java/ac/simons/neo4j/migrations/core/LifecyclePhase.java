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
package ac.simons.neo4j.migrations.core;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The phase in the lifecycle of a migrations flow in which a {@link Callback} is invoked.
 * <p>
 * This enum might become public as soon as neo4j-migrations supports Java based callbacks, too.
 *
 * @author Michael J. Simons
 * @since TBA
 */
enum LifecyclePhase {

	/**
	 * After connect but before any impersonation or the target database selection has been done.
	 * Can be used to create the target database for example.
	 */
	AFTER_CONNECT,

	/**
	 * Before Migrate runs.
	 */
	BEFORE_MIGRATE,
	/**
	 * After Migrate runs.
	 */
	AFTER_MIGRATE,

	/**
	 * Before Clean runs.
	 */
	BEFORE_CLEAN,
	/**
	 * After Clean runs.
	 */
	AFTER_CLEAN,

	/**
	 * Before Validate runs
	 */
	BEFORE_VALIDATE,
	/**
	 * After Validate runs
	 */
	AFTER_VALIDATE;

	static final Pattern LIFECYCLE_PATTERN;

	static {

		String group1 = Arrays.stream(LifecyclePhase.values())
				.map(LifecyclePhase::name)
				.map(LifecyclePhase::toCamelCase)
				.collect(Collectors.joining("|", "(", ")"));

		LIFECYCLE_PATTERN = Pattern.compile(group1 + "(?:__([\\w ]+))?(?:\\." + Defaults.CYPHER_SCRIPT_EXTENSION + ")?");
	}

	/**
	 * Transforms a string with words separated by {@literal _} into a camelCase string.
	 * @param value The value to transform
	 * @return the value in camelCase
	 */
	static String toCamelCase(String value) {
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
}
