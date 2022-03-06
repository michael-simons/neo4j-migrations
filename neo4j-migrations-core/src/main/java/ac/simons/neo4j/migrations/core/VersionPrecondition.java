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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.4.1
 */
final class VersionPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern HINT_PATTERN = Pattern.compile("(?i).*neo4j is(?<versions>(?:\\d|.)+)?");
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)?(\\.\\d+)?");

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
		}

		try {
			String rawVersions = matcher.group("versions");
			rawVersions = rawVersions.replace("or", ",");

			Set<String> formattedVersions = new TreeSet<>();
			for (String rawVersion : rawVersions.split(",")) {
				String version = rawVersion.trim();
				if (!VERSION_PATTERN.matcher(version).matches()) {
					throw new IllegalArgumentException();
				}
				formattedVersions.add("Neo4j/" + version);
			}
			return Optional.of(new VersionPrecondition(type, formattedVersions));
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new IllegalArgumentException(
				"Wrong version precondition. Usage: `<assume|assert> that neo4j is <versions>`. With <versions> being a comma separated list of major.minor.patch versions.");
		}
	}

	private final Set<String> requestedVersions;

	private VersionPrecondition(Type type, Set<String> requestedVersions) {
		super(type);
		this.requestedVersions = new TreeSet<>(requestedVersions);
	}

	@Override
	public boolean isSatisfied(MigrationContext migrationContext) {
		String serverVersion = migrationContext.getConnectionDetails().getServerVersion();
		return requestedVersions.contains(serverVersion);
	}

	Collection<String> getRequestedVersions() {
		return Collections.unmodifiableCollection(requestedVersions);
	}
}
