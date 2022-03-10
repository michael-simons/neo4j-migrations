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

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
final class EditionPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern CONDITION_PATTERN = Pattern.compile("(?i)^.*?edition is *(?<edition>\\w++)?$");

	/**
	 * Neo4j edition
	 */
	enum Edition {
		/**
		 * Constant for the enterprise edition.
		 */
		ENTERPRISE,
		/**
		 * Constant for the community edition.
		 */
		COMMUNITY,
		/**
		 * Constant for an unknown edition.
		 */
		UNKNOWN
	}

	/**
	 * Checks if the {@code hint} is matched by the {@link #CONDITION_PATTERN} and if so, tries to build a factory  for
	 * a corresponding precondition.
	 *
	 * @param hint The complete hint
	 * @return A factory for a precondition or an empty optional if this factory doesn't match the hint
	 */
	static Optional<Function<Type, Precondition>> tryToParse(String hint) {

		Matcher matcher = CONDITION_PATTERN.matcher(hint);
		if (!matcher.matches()) {
			return Optional.empty();
		} else {
			try {
				Edition edition = Edition.valueOf(matcher.group("edition").toUpperCase(Locale.ROOT));
				return Optional.of(type -> new EditionPrecondition(type, edition));
			} catch (Exception e) {
				throw new IllegalArgumentException(
					String.format(
							"Wrong edition precondition %s. Usage: `<assume|assert> that edition is <enterprise|community>`.",
							Precondition.formattedHint(hint)));
			}
		}
	}

	static Edition getEdition(ConnectionDetails connectionDetails) {

		try {
			String serverEdition = connectionDetails.getServerEdition();
			return serverEdition == null ? Edition.UNKNOWN : Edition.valueOf(serverEdition.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return Edition.UNKNOWN;
		}
	}

	private final Edition edition;

	private EditionPrecondition(Type type, Edition edition) {
		super(type);
		this.edition = edition;
	}

	@Override
	public boolean isMet(MigrationContext migrationContext) {
		return getEdition(migrationContext.getConnectionDetails()).equals(edition);
	}

	Edition getEdition() {
		return edition;
	}

	@Override
	public String toString() {
		return String.format("// %s that edition is %s", getType().keyword(), getEdition().name());
	}
}
