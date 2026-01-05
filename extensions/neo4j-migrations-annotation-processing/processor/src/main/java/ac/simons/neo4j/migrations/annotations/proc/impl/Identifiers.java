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
package ac.simons.neo4j.migrations.annotations.proc.impl;

/**
 * Copy of
 * {@code org.springframework.data.neo4j.core.mapping.DefaultNeo4jPersistentProperty#deriveRelationshipType(String)}
 * Originally licensed under Apache License, Version 2.0 (the "License"), written by
 * Michael J. Simons
 *
 * @author Michael J. Simons
 */
final class Identifiers {

	private Identifiers() {
	}

	static String deriveRelationshipType(String name) {

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException();
		}

		StringBuilder sb = new StringBuilder();

		int codePoint;
		int previousIndex = 0;
		int i = 0;
		while (i < name.length()) {
			codePoint = name.codePointAt(i);
			if (Character.isLowerCase(codePoint)) {
				if (i > 0 && !Character.isLetter(name.codePointAt(previousIndex))) {
					sb.append("_");
				}
				codePoint = Character.toUpperCase(codePoint);
			}
			else if (!sb.isEmpty()) {
				sb.append("_");
			}
			sb.append(Character.toChars(codePoint));
			previousIndex = i;
			i += Character.charCount(codePoint);
		}
		return sb.toString();
	}

}
