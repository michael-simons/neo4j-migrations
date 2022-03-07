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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
final class VersionPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern CONDITION_PATTERN = Pattern.compile("(?i).*?version is(?<versions>.+)?");
	private static final Pattern VERSION_SUB_PATTERN = Pattern.compile("\\d+(\\.\\d+)?(\\.\\d+)?");

	/**
	 * Checks if the {@code hint} is matched by the {@link #CONDITION_PATTERN} and if so, tries to create a factory for
	 * a corresponding precondition.
	 *
	 * @param hint The complete hint
	 * @return A factory for a precondition or an empty optional if this factory doesn't match the hint
	 */
	static Optional<Function<Type, Precondition>> tryToParse(String hint) {

		Matcher matcher = CONDITION_PATTERN.matcher(hint);
		if (!matcher.matches()) {
			return Optional.empty();
		}

		try {
			String rawVersions = matcher.group("versions");
			rawVersions = rawVersions.replace("or", ",");

			Set<String> formattedVersions = new TreeSet<>();
			for (String rawVersion : rawVersions.split(",")) {
				String version = rawVersion.trim();
				if (!VERSION_SUB_PATTERN.matcher(version).matches()) {
					throw new IllegalArgumentException();
				}
				formattedVersions.add("Neo4j/" + version);
			}
			return Optional.of(type -> new VersionPrecondition(type, formattedVersions));
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new IllegalArgumentException(
				"Wrong version precondition. Usage: `<assume|assert> that version is <versions>`. With <versions> being a comma separated list of major.minor.patch versions.");
		}
	}

	private final Set<String> versions;

	private VersionPrecondition(Type type, Set<String> versions) {
		super(type);
		this.versions = new TreeSet<>(versions);
	}

	@Override
	public boolean isSatisfied(MigrationContext migrationContext) {
		String serverVersion = migrationContext.getConnectionDetails().getServerVersion();
		return versions.stream().anyMatch(serverVersion::contains);
	}

	Collection<String> getVersions() {
		return Collections.unmodifiableCollection(versions);
	}

	@Override
	public String toString() {
		return String.format("// %s that version is %s", getType().keyword(),
			versions.stream().map(s -> s.replace("Neo4j/", "")).collect(
				Collectors.joining(", ")).trim());
	}
}
