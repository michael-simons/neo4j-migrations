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
import java.util.List;
import java.util.Locale;
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
		ASSUMPTION("// *assume(?: in (?:schema|target))? that.*"),
		/**
		 * Preconditions that are asserted and are not satisfied will halt the migrations at the given migration.
		 */
		ASSERTION("// *assert(?: in (?:schema|target))? that.*");

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
				return null;
			}
		}
	}

	Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)?(\\.\\d+)?");
	List<String> SUPPORTED_EDITIONS = Arrays.asList("COMMUNITY", "ENTERPRISE");

	static Precondition parse(String in) {

		Type type = Type.of(in);
		if (type == null) {
			return null;
		}

		Matcher versionMatcher = Pattern.compile(".*neo4j is(?<versions>.+)?", Pattern.CASE_INSENSITIVE).matcher(in);
		if (versionMatcher.matches()) {
			try {
				String versionGroup = versionMatcher.group("versions");
				versionGroup = versionGroup.replace("or", "").replace(" ", "");
				Set<String> versions = Arrays.stream(versionGroup.split(","))
						.peek(version -> {
							if (!VERSION_PATTERN.matcher(version).matches()) {
								throw new IllegalArgumentException(); // bubbles up to the outer catch
							}
						})
						.map(version -> "Neo4j/" + version)
						.collect(Collectors.toSet());
				return new VersionPrecondition(type, versions);
			} catch (Exception e) {
				throw new IllegalArgumentException("Wrong version precondition. Usage: `<assume|assert> that neo4j is <versions>. With <versions> being a comma separated list.`");
			}
		}

		Matcher editionMatcher = Pattern.compile(".*edition is(?<edition>.+)?", Pattern.CASE_INSENSITIVE).matcher(in);
		if (editionMatcher.matches()) {
			try {
				String editionGroup = editionMatcher.group("edition");
				String editionValue = editionGroup.replace(" ", "").toUpperCase(Locale.ROOT);
				if (!SUPPORTED_EDITIONS.contains(editionValue)) {
					throw new IllegalArgumentException(); // bubbles up to the outer catch
				}
				HBD.Edition edition = HBD.Edition.valueOf(editionValue);
				return new EditionPrecondition(type, edition);
			} catch (Exception e) {
				throw new IllegalArgumentException("Wrong edition precondition. Usage: `<assume|assert> that edition is <enterprise|community>`");
			}

		}

		Matcher cypherMatcher = Pattern.compile("// *(assert|assume)(?<database> in (target|schema))? that(?<cypher>.+)?", Pattern.CASE_INSENSITIVE).matcher(in);
		if (cypherMatcher.matches()) {
			try {
				String cypherGroup = cypherMatcher.group("cypher");
				if (cypherGroup == null || cypherGroup.trim().length() == 0) {
					throw new IllegalArgumentException();  // bubbles up to the outer catch
				}
				String databaseGroup = cypherMatcher.group("database");
				if (databaseGroup != null) {
					databaseGroup = databaseGroup.replace("in", "").replace(" ", "").trim();
				}

				return new CypherPrecondition(type, cypherGroup, databaseGroup);
			} catch (Exception e) {
				throw new IllegalArgumentException("Wrong Cypher precondition. Usage: `<assume|assert> [in <target|schema>] that <cypher statement>`");
			}
		}
		// since Cypher precondition catches more or less everything else, we should never be here, but hey, let's be friendly
		throw new IllegalArgumentException("Wrong precondition. Please have a look at <insert docs reference here>.");
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
