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
package ac.simons.neo4j.migrations.core;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @since 1.4.1
 */
interface Precondition {

	enum Type {
		ASSUMPTION,
		ASSERTION
	}

	static Precondition parse(String in) {
		Predicate<String> assumptionPattern = Pattern.compile("// *assume that*", Pattern.CASE_INSENSITIVE).asPredicate();
		Predicate<String> assertionPattern = Pattern.compile("// *assert that*", Pattern.CASE_INSENSITIVE).asPredicate();
		Type type = null;

		if (assumptionPattern.test(in)) {
			type = Type.ASSUMPTION;
		} else if (assertionPattern.test(in)) {
			type = Type.ASSERTION;
		} else {
			throw new IllegalArgumentException("Wrong precondition keyword. Allowed: `[assume, assert] that`");
		}

		Matcher versionMatcher = Pattern.compile(".*neo4j is (?<versions>.+)").matcher(in);
		if (versionMatcher.matches()) {
			String versionGroup = versionMatcher.group("versions");
			versionGroup = versionGroup.replaceAll("or", "").replaceAll(" ", "");
			Set<String> versions = Arrays.stream(versionGroup.split(","))
					.map(version -> "Neo4j/" + version)
					.collect(Collectors.toSet());
			return new VersionPrecondition(type, versions);
		}

		// todo fine tune regex to avoid multiple editions
		Matcher editionMatcher = Pattern.compile(".*edition is (?<edition>.+)").matcher(in);
		if (editionMatcher.matches()) {
			String editionGroup = editionMatcher.group("edition");
			ConnectionDetails.Edition edition = ConnectionDetails.Edition.from(editionGroup.replaceAll(" ", ""));

			return new EditionPrecondition(type, edition);

		}

		return null;
	}

	boolean isSatisfied(MigrationContext migrationContext);

	Type getType();
}
