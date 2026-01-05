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

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.internal.Neo4jVersionComparator;

/**
 * A precondition preventing unwanted Neo4j versions.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
final class VersionPrecondition extends AbstractPrecondition implements Precondition {

	private static final String MMP_PATTERN = "\\d++(\\.\\d++)*+";

	private static final Pattern CONDITION_PATTERN = Pattern
		.compile("(?i)^ *+// *+(?:assume|assert) that version is *+(?<versions>" + MMP_PATTERN + "[\\w\\s.,]*+)?.*+$");

	private static final Pattern CONDITION_RANGE_PATTERN = Pattern
		.compile("(?i)^ *+// *+(?:assume|assert) that version is (lt|ge) (?<versions>" + MMP_PATTERN + ").*+$");

	private static final Pattern VERSION_SUB_PATTERN = Pattern.compile(MMP_PATTERN);

	private final Mode mode;

	private final Set<String> versions;

	private VersionPrecondition(Type type, final Mode mode, Set<String> versions) {
		super(type);
		this.mode = mode;
		this.versions = versions;
	}

	/**
	 * Checks if the {@code hint} is matched by the {@link #CONDITION_PATTERN} and if so,
	 * tries to create a factory for a corresponding precondition.
	 * @param hint the complete hint
	 * @return a factory for a precondition or an empty optional if this factory doesn't
	 * match the hint
	 */
	static Optional<Function<Type, Precondition>> tryToParse(String hint) {

		Matcher matcher = CONDITION_RANGE_PATTERN.matcher(hint);
		Mode mode;
		if (matcher.matches()) {
			mode = Mode.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
		}
		else {
			matcher = CONDITION_PATTERN.matcher(hint);
			if (!matcher.find()) {
				return Optional.empty();
			}
			mode = Mode.EXACT;
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
			return Optional.of(type -> new VersionPrecondition(type, mode, formattedVersions));
		}
		catch (NullPointerException | IllegalArgumentException ex) {
			throw new IllegalArgumentException(String.format(
					"Wrong version precondition %s. Usage: `<assume|assert> that version is <versions>`. With <versions> being a comma separated list of major.minor.patch versions.",
					Precondition.formattedHint(hint)));
		}
	}

	@Override
	public boolean isMet(MigrationContext migrationContext) {
		String serverVersion = migrationContext.getConnectionDetails().getServerVersion();
		if (this.mode == Mode.EXACT) {
			return this.versions.stream().anyMatch(serverVersion::startsWith);
		}
		else {
			return this.versions.stream().findFirst().map(version -> {
				int result = new Neo4jVersionComparator().compare(serverVersion, version);
				return (this.mode == Mode.LT) == (result < 0);
			}).orElse(false);
		}
	}

	Collection<String> getVersions() {
		return Collections.unmodifiableCollection(this.versions);
	}

	@Override
	public String toString() {
		return String.format("// %s that version is %s%s", getType().keyword(),
				(this.mode == Mode.EXACT) ? "" : (this.mode.name().toLowerCase(Locale.ROOT) + " "),
				this.versions.stream().map(s -> s.replace("Neo4j/", "")).collect(Collectors.joining(", ")).trim());
	}

	/**
	 * How to match versions.
	 */
	enum Mode {

		/** Everything lower than the given single pattern. */
		LT,
		/** Everything higher than the given single pattern. */
		GE,
		/** Exact. */
		EXACT

	}

}
