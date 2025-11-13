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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.cypherdsl.support.schema_name.SchemaNames;

/**
 * Simplified Neo4j version, fuzzy if you want so (just looking at Major.Minor, not the
 * patches).
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public enum Neo4jVersion {

	/**
	 * Constant for an undefined version.
	 */
	UNDEFINED,
	/**
	 * Constant for everything Neo4j 3.5 and earlier.
	 */
	V3_5(true, Neo4jVersion.OLD_SHOW_CONSTRAINTS, Neo4jVersion.OLD_SHOW_INDEXES),
	/**
	 * Constant for everything Neo4j 4.0.
	 */
	V4_0(true, Neo4jVersion.OLD_SHOW_CONSTRAINTS, Neo4jVersion.OLD_SHOW_INDEXES),
	/**
	 * Constant for everything Neo4j 4.1.
	 */
	V4_1(true, Neo4jVersion.OLD_SHOW_CONSTRAINTS, Neo4jVersion.OLD_SHOW_INDEXES),
	/**
	 * Constant for everything Neo4j 4.2.
	 */
	V4_2(true, "SHOW CONSTRAINTS", Neo4jVersion.OLD_SHOW_INDEXES),
	/**
	 * Constant for everything Neo4j 4.3.
	 */
	V4_3(true, Neo4jVersion.NEW_SHOW_CONSTRAINTS, Neo4jVersion.NEW_SHOW_INDEXES),
	/**
	 * Constant for everything Neo4j 4.4.
	 */
	V4_4,
	/**
	 * Constant for everything Neo4j 5.
	 */
	V5,
	/**
	 * Constant when we assume the latest version.
	 */
	LATEST;

	private static final String OLD_SHOW_CONSTRAINTS = "CALL db.constraints()";

	private static final String NEW_SHOW_CONSTRAINTS = "SHOW CONSTRAINTS YIELD *";

	private static final String OLD_SHOW_INDEXES = "CALL db.indexes()";

	private static final String NEW_SHOW_INDEXES = "SHOW INDEXES YIELD *";

	/**
	 * A set of versions that have a concept of idempotent constraint / index statements.
	 */
	private static final Set<Neo4jVersion> WITH_IDEM_POTENCY = EnumSet.complementOf(EnumSet.of(V3_5, V4_0));

	private static final Set<Neo4jVersion> WITH_TEXT_INDEXES = EnumSet
		.complementOf(EnumSet.of(V3_5, V4_0, V4_1, V4_2, V4_3, UNDEFINED));

	private static final Set<Neo4jVersion> SERIES_3 = EnumSet.of(V3_5);

	private static final Set<Neo4jVersion> SERIES_4 = Arrays.stream(Neo4jVersion.values())
		.filter(v -> v.name().startsWith("V4_"))
		.collect(Collectors.collectingAndThen(Collectors.toSet(), EnumSet::copyOf));

	private static final Set<Neo4jVersion> SERIES_5 = Arrays.stream(Neo4jVersion.values())
		.filter(v -> v.name().startsWith("V5"))
		.collect(Collectors.collectingAndThen(Collectors.toSet(), EnumSet::copyOf));

	private final boolean priorTo44;

	private final String showConstraints;

	private final String showIndexes;

	Neo4jVersion() {
		this(false, NEW_SHOW_CONSTRAINTS, NEW_SHOW_INDEXES);
	}

	Neo4jVersion(boolean priorTo44, String showConstraints, String showIndexes) {
		this.priorTo44 = priorTo44;
		this.showConstraints = showConstraints;
		this.showIndexes = showIndexes;
	}

	/**
	 * Parses a version string in a lenient way. Only Major and Minor versions are looked
	 * at.
	 * @param version a version string
	 * @return a version
	 */
	public static Neo4jVersion of(String version) {

		String value = (version != null) ? version.replaceFirst("(?i)Neo4j[/:]", "") : null;
		if (value == null) {
			return UNDEFINED;
		}
		else if (value.startsWith("3.5")) {
			return V3_5;
		}
		else if (value.startsWith("4.0")) {
			return V4_0;
		}
		else if (value.startsWith("4.1")) {
			return V4_1;
		}
		else if (value.startsWith("4.2")) {
			return V4_2;
		}
		else if (value.startsWith("4.3")) {
			return V4_3;
		}
		else if (value.startsWith("4.4")) {
			return V4_4;
		}
		else if (value.startsWith("5.")) {
			return V5;
		}
		else {
			return LATEST;
		}
	}

	@Override
	public String toString() {
		return name().replace("V", "").replace("_", ".");
	}

	/**
	 * {@return true if this is a version prior to 4.4}
	 */
	public boolean isPriorTo44() {
		return this.priorTo44;
	}

	/**
	 * {@return true if this version has idempotent operations}
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") // That might be, but I prefer
														// readability
	public boolean hasIdempotentOperations() {
		return WITH_IDEM_POTENCY.contains(this);
	}

	/**
	 * {@return true if this version has text indexes}
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") // That might be, but I prefer
														// readability
	public boolean hasTextIndexes() {
		return WITH_TEXT_INDEXES.contains(this);
	}

	/**
	 * Returns true if this version supports specifying options on constraints and
	 * indexes.
	 * @return true if this version supports specifying options on constraints and indexes
	 * @since 1.13.0
	 */
	public boolean supportsSchemaOptions() {
		return this.compareTo(Neo4jVersion.V4_1) > 0;
	}

	/**
	 * Returns the command recommended for this specific version to get a list of all
	 * known constraints.
	 * @return a cypher command retrieving constraints
	 */
	public String getShowConstraints() {
		return this.showConstraints;
	}

	/**
	 * Returns the command recommended for this specific version to get a list of all
	 * known indexes.
	 * @return a cypher command retrieving indexes
	 */
	public String getShowIndexes() {
		return this.showIndexes;
	}

	/**
	 * Returns the major version of the database.
	 * @return the major version of the database
	 * @since 1.10.0
	 */
	public int getMajorVersion() {
		if (this == LATEST) {
			return -1;
		}
		else if (SERIES_3.contains(this)) {
			return 3;
		}
		else if (SERIES_4.contains(this)) {
			return 4;
		}
		else if (SERIES_5.contains(this)) {
			return 5;
		}
		throw new IllegalStateException("Unknown major version");
	}

	/**
	 * Returns the minor version or {@code -1} if it can be determined.
	 * @return the minor version or {@code -1} if it can be determined
	 * @since 1.11.0
	 */
	public int getMinorVersion() {
		if (this == LATEST || this == UNDEFINED) {
			return -1;
		}
		String[] parts = this.name().split("_");
		if (parts.length != 2) {
			return -1;
		}
		return Integer.parseInt(parts[1]);
	}

	/**
	 * Escapes the string {@literal potentiallyNonIdentifier} in all cases when it's not a
	 * valid Cypher identifier, fitting the given version.
	 * @param potentiallyNonIdentifier a value to escape, must not be {@literal null} or
	 * blank
	 * @return the sanitized and quoted value or the same value if no change is necessary.
	 * @since 1.11.0
	 */
	public String sanitizeSchemaName(String potentiallyNonIdentifier) {

		if (potentiallyNonIdentifier == null || potentiallyNonIdentifier.isEmpty()) {
			return potentiallyNonIdentifier;
		}

		int major;
		try {
			major = getMajorVersion();
		}
		catch (IllegalStateException ex) {
			major = -1;
		}
		int minor = getMinorVersion();

		return SchemaNames.sanitize(potentiallyNonIdentifier, false, major, minor)
			.orElseThrow(NoSuchElementException::new);
	}

}
