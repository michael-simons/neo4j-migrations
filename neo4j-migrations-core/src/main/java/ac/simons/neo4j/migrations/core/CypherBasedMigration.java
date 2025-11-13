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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.internal.Strings;

/**
 * A cypher script based migration.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
final class CypherBasedMigration extends AbstractCypherBasedMigration implements MigrationWithPreconditions {

	/**
	 * In case a migration with the {@link #getSource() source} with different
	 * preconditions is available, we treat those as alternatives.
	 */
	private List<String> alternativeChecksums = Collections.emptyList();

	@SuppressWarnings("squid:S3077") // This will always be an immutable instance
	private volatile Optional<String> checksumOfNonePreconditions;

	CypherBasedMigration(ResourceContext context) {
		super(CypherResource.of(context));
	}

	// The whole point of the optional is in fact to deal with non-null
	// String is not enough, as null is a valid value in this case and comparing
	// to it would defeat the purpose of caching it.
	@SuppressWarnings({ "OptionalAssignedToNull", "squid:S2789" })
	Optional<String> getChecksumWithoutPreconditions() {

		Optional<String> availableChecksum = this.checksumOfNonePreconditions;
		if (availableChecksum == null) {
			synchronized (this) {
				availableChecksum = this.checksumOfNonePreconditions;
				if (availableChecksum == null) {
					this.checksumOfNonePreconditions = computeChecksumWithoutPreconditions();
					availableChecksum = this.checksumOfNonePreconditions;
				}
			}
		}
		return availableChecksum;
	}

	private Optional<String> computeChecksumWithoutPreconditions() {

		// On JDK17 there can be only one, but alas, we aren't there yet.
		if (!(this.cypherResource instanceof DefaultCypherResource)) {
			return Optional.empty();
		}
		List<String> statements = ((DefaultCypherResource) this.cypherResource).getStatements()
			.stream()
			.map(statement -> {
				String quotedPatterns = DefaultCypherResource.getSingleLineComments(statement)
					.filter(c -> Precondition.parse(c).isPresent())
					.map(String::trim)
					.map(Pattern::quote)
					.collect(Collectors.joining("|"));

				return quotedPatterns.isEmpty() ? statement
						: statement.replaceAll("(" + quotedPatterns + ")" + Strings.LINE_DELIMITER, "");
			})
			.toList();
		return Optional.of(DefaultCypherResource.computeChecksum(statements));
	}

	@Override
	public List<String> getAlternativeChecksums() {

		Optional<String> additionalAlternativeChecksum = getChecksumWithoutPreconditions();
		if (getChecksum().equals(additionalAlternativeChecksum)) {
			return Collections.unmodifiableList(this.alternativeChecksums);
		}

		List<String> alternateChecksums = new ArrayList<>(this.alternativeChecksums);
		additionalAlternativeChecksum.ifPresent(alternateChecksums::add);
		return alternateChecksums;
	}

	@Override
	public void setAlternativeChecksums(List<String> alternativeChecksums) {

		Objects.requireNonNull(alternativeChecksums);
		this.alternativeChecksums = new ArrayList<>(alternativeChecksums);
	}

	@Override
	public List<Precondition> getPreconditions() {
		return Precondition.in(this.cypherResource);
	}

}
