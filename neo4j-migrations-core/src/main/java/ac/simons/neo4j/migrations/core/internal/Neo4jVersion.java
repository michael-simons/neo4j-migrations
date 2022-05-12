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
package ac.simons.neo4j.migrations.core.internal;

import java.util.EnumSet;
import java.util.Set;

/**
 * Simplified Neo4j version, fuzzy if you want so (just looking at Major.Minor, not the patches).
 *
 * @author Michael J. Simons
 * @since TBA
 */
public enum Neo4jVersion {

	V3_5(true),
	V4_0(true),
	V4_1(true),
	V4_2(true),
	V4_3(true),
	V4_4,
	LATEST,
	UNDEFINED;

	public static final Set<Neo4jVersion> NO_IDEM_POTENCY = EnumSet.of(V3_5, V4_0);
	public static final Set<Neo4jVersion> RANGE_41_TO_43 = EnumSet.of(V4_1, V4_2, V4_3);
	public static final Set<Neo4jVersion> RANGE_41_TO_42 = EnumSet.of(V4_1, V4_2);

	public static Neo4jVersion of(String version) {

		String value = version == null ? null : version.replaceFirst("(?i)Neo4j/", "");
		if (value == null) {
			return UNDEFINED;
		} else if (value.startsWith("3.5")) {
			return V3_5;
		} else if (value.startsWith("4.0")) {
			return V4_0;
		} else if (value.startsWith("4.1")) {
			return V4_1;
		} else if (value.startsWith("4.2")) {
			return V4_2;
		} else if (value.startsWith("4.3")) {
			return V4_3;
		} else if (value.startsWith("4.4")) {
			return V4_4;
		} else {
			return LATEST;
		}
	}

	private final boolean priorTo44;

	Neo4jVersion() {
		this(false);
	}

	Neo4jVersion(boolean priorTo44) {
		this.priorTo44 = priorTo44;
	}

	@Override
	public String toString() {
		return name().replace("V", "").replace("_", ".");
	}

	public boolean isPriorTo44() {
		return priorTo44;
	}
}
