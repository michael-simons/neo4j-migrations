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
package ac.simons.neo4j.migrations.core.internal;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Compares two Neo4j versions strings.
 *
 * @author Michael J. Simons
 * @since 1.5.0
 */
public final class Neo4jVersionComparator implements Comparator<String> {

	static String[] sanitize(String in) {
		return in.replaceFirst("(?i)^Neo4j/", "").replaceFirst("-[\\w.]+$", "").split("\\.");
	}

	@Override
	public int compare(String o1, String o2) {
		int c = o1.compareTo(o2);
		if (c == 0) {
			return c;
		}

		String[] a = sanitize(o1);
		String[] b = sanitize(o2);
		if (a.length < b.length) {
			a = Arrays.copyOf(a, b.length);
		}
		else if (a.length > b.length) {
			b = Arrays.copyOf(b, a.length);
		}

		for (int i = 0; i < a.length; ++i) {
			Integer d1 = Integer.valueOf((a[i] != null) ? a[i] : "0");
			Integer d2 = Integer.valueOf((b[i] != null) ? b[i] : "0");
			c = d1.compareTo(d2);
			if (c != 0) {
				return c;
			}
		}

		return 0;
	}

}
