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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.EnumSet;
import java.util.Set;

/**
 * Simplified Neo4j version.
 *
 * @author Michael J. Simons
 * @since TBA
 */
enum Version {

	V3_5,
	V4_0,
	V4_1,
	V4_2,
	V4_3,
	V4_4,
	LATEST;

	static final Set<Version> NO_IDEM_POTENCY = EnumSet.of(V3_5, V4_0);
	static final Set<Version> RANGE_41_TO_43 = EnumSet.of(V4_1, V4_2, V4_3);
	static final Set<Version> RANGE_41_TO_42 = EnumSet.of(V4_1, V4_2);

	static Version of(String version) {

		if (version.startsWith("3.5")) {
			return V3_5;
		} else if (version.startsWith("4.0")) {
			return V4_0;
		} else if (version.startsWith("4.1")) {
			return V4_1;
		} else if (version.startsWith("4.2")) {
			return V4_2;
		} else if (version.startsWith("4.3")) {
			return V4_3;
		} else if (version.startsWith("4.4")) {
			return V4_4;
		} else {
			return LATEST;
		}
	}

	@Override
	public String toString() {
		return name().replace("V", "").replace("_", ".");
	}
}
