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

import java.util.EnumSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.internal.Strings;

/**
 * The phase in the lifecycle of a migrations flow in which a {@link Callback} is invoked.
 * <p>
 * This enum might become public as soon as neo4j-migrations supports Java-based
 * callbacks, too.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
enum LifecyclePhase {

	/**
	 * Callbacks in that phase are only called once and are also the only ones that are
	 * called with the schema database and not the target database, so they won't require
	 * the target database to be present. Also, no user impersonation will be performed.
	 */
	BEFORE_FIRST_USE,

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
	 * Before Validate runs.
	 */
	BEFORE_VALIDATE,
	/**
	 * After Validate runs.
	 */
	AFTER_VALIDATE,

	/**
	 * Before Info runs.
	 */
	BEFORE_INFO,
	/**
	 * After Info runs.
	 */
	AFTER_INFO;

	static final Pattern LIFECYCLE_PATTERN;

	private static final EnumSet<LifecyclePhase> ALL_VALUES = EnumSet.allOf(LifecyclePhase.class);

	static {

		String group1 = ALL_VALUES.stream().map(LifecyclePhase::toCamelCase).collect(Collectors.joining("|", "(", ")"));

		LIFECYCLE_PATTERN = Pattern
			.compile(group1 + "(?:__([\\w ]+))?(?:\\." + Defaults.CYPHER_SCRIPT_EXTENSION + ")?");
	}

	static boolean canParse(String pathOrUrl) {
		return LIFECYCLE_PATTERN.matcher(pathOrUrl).find();
	}

	static LifecyclePhase fromCamelCase(String value) {

		return ALL_VALUES.stream()
			.filter(phase -> phase.toCamelCase().equals(value))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("No such lifecycle phase: " + value));
	}

	String toCamelCase() {
		return Strings.toCamelCase(name());
	}

	String readable() {
		return name().toLowerCase(Locale.ENGLISH).replace("_", " ");
	}

}
