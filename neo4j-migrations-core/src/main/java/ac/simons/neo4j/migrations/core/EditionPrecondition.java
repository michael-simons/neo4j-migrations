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

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A precondation preventing unwanted editions.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
final class EditionPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern CONDITION_PATTERN = Pattern
		.compile("(?i)^ *+// *+(?:assume|assert) that edition is *(?<edition>\\w++)?$");

	private final Neo4jEdition edition;

	private EditionPrecondition(Type type, Neo4jEdition edition) {
		super(type);
		this.edition = edition;
	}

	/**
	 * Checks if the {@code hint} is matched by the {@link #CONDITION_PATTERN} and if so,
	 * tries to build a factory for a corresponding precondition.
	 * @param hint the complete hint
	 * @return a factory for a precondition or an empty optional if this factory doesn't
	 * match the hint
	 */
	static Optional<Function<Type, Precondition>> tryToParse(String hint) {

		Matcher matcher = CONDITION_PATTERN.matcher(hint);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		else {
			try {
				Neo4jEdition edition = Neo4jEdition.of(matcher.group("edition"), false);
				return Optional.of(type -> new EditionPrecondition(type, edition));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(String.format(
						"Wrong edition precondition %s. Usage: `<assume|assert> that edition is <enterprise|community>`.",
						Precondition.formattedHint(hint)));
			}
		}
	}

	static Neo4jEdition getEdition(ConnectionDetails connectionDetails) {

		return Neo4jEdition.of(connectionDetails.getServerEdition());
	}

	@Override
	public boolean isMet(MigrationContext migrationContext) {
		return getEdition(migrationContext.getConnectionDetails()).equals(this.edition);
	}

	Neo4jEdition getEdition() {
		return this.edition;
	}

	@Override
	public String toString() {
		return String.format("// %s that edition is %s", getType().keyword(), getEdition().name());
	}

}
