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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.4.1
 */
final class EditionPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern HINT_PATTERN = Pattern.compile("(?i).*edition is *(?<edition>\\w+)?");

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
	 * Checks if the {@code hint} is matched by the {@link #HINT_PATTERN} and if so, tries to build a fitting precondition.
	 *
	 * @param type The type of the precondition to be created
	 * @param hint The complete hint
	 * @return A precondition or an empty optional if this factory doesn't match the hint
	 */
	static Optional<Precondition> of(Precondition.Type type, String hint) {

		Matcher matcher = HINT_PATTERN.matcher(hint);
		if (!matcher.matches()) {
			return Optional.empty();
		} else {
			try {
				String editionGroup = matcher.group("edition");
				String editionValue = editionGroup.toUpperCase(Locale.ROOT);
				Edition edition = Edition.valueOf(editionValue);
				return Optional.of(new EditionPrecondition(type, edition));
			} catch (Exception e) {
				throw new IllegalArgumentException(
					"Wrong edition precondition. Usage: `<assume|assert> that edition is <enterprise|community>`");
			}
		}
	}

	static Edition getEdition(ConnectionDetails connectionDetails) {

		String serverVersion = connectionDetails.getServerVersion().toUpperCase(Locale.ROOT);
		if (serverVersion.contains(Edition.ENTERPRISE.name())) {
			return Edition.ENTERPRISE;
		} else if (serverVersion.contains(Edition.COMMUNITY.name())) {
			return Edition.COMMUNITY;
		} else {
			return Edition.UNKNOWN;
		}
	}

	private final Edition edition;

	private EditionPrecondition(Type type, Edition edition) {
		super(type);
		this.edition = edition;
	}

	@Override
	public boolean isSatisfied(MigrationContext migrationContext) {
		return getEdition(migrationContext.getConnectionDetails()).equals(edition);
	}

	Edition getEdition() {
		return edition;
	}
}
