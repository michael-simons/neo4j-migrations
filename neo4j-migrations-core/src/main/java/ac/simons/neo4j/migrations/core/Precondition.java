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
package ac.simons.neo4j.migrations.core;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A precondition can be needs to be met after discovering migrations and prior to
 * building changes. If it isn't met, the outcome depends on its
 * {@link Precondition#getType()}: If it's an unmet {@link Type#ASSERTION}, discovering of
 * migrations will stop and not check any migrations or preconditions further. If it's an
 * unmet {@link Type#ASSUMPTION}, the migration to which the precondition belongs, will be
 * discarded.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
interface Precondition {

	/**
	 * Tries to parse a precondition.
	 * @param in the single line comment
	 * @return an empty optional if it's not something that is meant to be a precondition
	 * or a precondition
	 * @throws IllegalArgumentException if {@literal in} looks like a precondition but
	 * cannot be successfully parsed
	 */
	static Optional<Precondition> parse(String in) {

		return Type.of(in).flatMap(type -> {
			Optional<Function<Type, Precondition>> producer;

			producer = VersionPrecondition.tryToParse(in);
			if (producer.isPresent()) {
				return producer.map(f -> f.apply(type));
			}

			producer = EditionPrecondition.tryToParse(in);
			if (producer.isPresent()) {
				return producer.map(f -> f.apply(type));
			}

			producer = QueryPrecondition.tryToParse(in);
			if (producer.isPresent()) {
				return producer.map(f -> f.apply(type));
			}

			throw new IllegalArgumentException("Wrong precondition " + formattedHint(in)
					+ ". Supported are `<assume|assert> (that <edition|version>)|q' <cypherQuery>)`.");
		});
	}

	/**
	 * Gets all conditions in the given resource.
	 * @param cypherResource the resource to extract preconditions from
	 * @return a list of preconditions
	 */
	static List<Precondition> in(CypherResource cypherResource) {
		return cypherResource.getSingleLineComments()
			.stream()
			.map(Precondition::parse)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.toList();
	}

	static String formattedHint(String hint) {
		return "`" + hint.replace("//", "").trim() + "`";
	}

	/**
	 * Checks whether this precondition is satisfied in the given context.
	 * @param migrationContext the context in which this condition should be satisfied
	 * @return true, if the precondition can be satisfied in the given context.
	 */
	boolean isMet(MigrationContext migrationContext);

	/**
	 * Every precondition must either be an {@link Type#ASSUMPTION assumption} or an
	 * {@link Type#ASSERTION}, and will be treated accordingly (if an assumption is not
	 * satisfied, a given migration will be skipped, if an assertion is not satisfied, it
	 * will halt the chain of migration at the given migration).
	 * @return the type of the precondition.
	 */
	Type getType();

	/**
	 * Type describing how a precondition is dealt with (assumed or asserted).
	 */
	enum Type {

		/**
		 * Preconditions that are assumed and are not satisfied will lead to migrations
		 * being skipped.
		 */
		ASSUMPTION("assume"),
		/**
		 * Preconditions that are asserted and are not satisfied will halt the migrations
		 * at the given migration.
		 */
		ASSERTION("assert");

		private final String keyword;

		private final Pattern pattern;

		Type(String keyword) {
			this.keyword = keyword;
			this.pattern = Pattern.compile("(?i)// *" + keyword + "(?: in (?:schema|target))? (that|q').*");
		}

		static Optional<Type> of(String value) {
			if (ASSUMPTION.pattern.matcher(value).find()) {
				return Optional.of(Type.ASSUMPTION);
			}
			else if (ASSERTION.pattern.matcher(value).find()) {
				return Optional.of(Type.ASSERTION);
			}
			else {
				return Optional.empty();
			}
		}

		String keyword() {
			return this.keyword;
		}

	}

}
