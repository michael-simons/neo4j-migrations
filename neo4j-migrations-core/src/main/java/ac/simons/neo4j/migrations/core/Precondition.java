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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.4.1
 */
interface Precondition {

	/**
	 * Type describing how a precondition is dealt with (assumed or asserted).
	 */
	enum Type {
		/**
		 * Preconditions that are assumed and are not satisfied will lead to migrations being skipped.
		 */
		ASSUMPTION("// *assume (?: in (?:schema|target))? that*"),
		/**
		 * Preconditions that are asserted and are not satisfied will halt the migrations at the given migration.
		 */
		ASSERTION("// *assert that*");

		private final Pattern pattern;

		Type(String pattern) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		}

		static Type of(String value) {
			if (ASSUMPTION.pattern.matcher(value).find()) {
				return Type.ASSUMPTION;
			} else if (ASSERTION.pattern.matcher(value).find()) {
				return Type.ASSERTION;
			} else {
				throw new IllegalArgumentException("Wrong precondition keyword. Allowed: `[assume, assert] that`");
			}
		}
	}

	static Precondition parse(String in) {

		Type type = Type.of(in);
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

	/**
	 * Checks whether this precondition is satisfied in the given context.
	 *
	 * @param migrationContext The context in which this condition should be satisfied
	 * @return True, if the precondition can be satisfied in the given context.
	 */
	boolean isSatisfied(MigrationContext migrationContext);

	/**
	 * Every precondition must either be an {@link Type#ASSUMPTION assumption} or an {@link Type#ASSERTION}, and will
	 * be treated accordingly (if an assumption is not satisfied, a given migration will be skipped, if an assertion is
	 * not satisfied, it will halt the chain of migration at the given migration).
	 *
	 * @return The type of the precondition.
	 */
	Type getType();
}
