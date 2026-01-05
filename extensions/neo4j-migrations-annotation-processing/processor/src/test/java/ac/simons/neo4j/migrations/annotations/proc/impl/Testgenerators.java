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

import ac.simons.neo4j.migrations.annotations.proc.CatalogNameGenerator;

/**
 * @author Michael J. Simons
 */
public final class Testgenerators {

	/**
	 * Not visible.
	 */
	private static final class HiddenA implements CatalogNameGenerator {

		@Override
		public String getCatalogName() {
			return null;
		}

	}

	/**
	 * Not visible.
	 */
	static class HiddenB implements CatalogNameGenerator {

		@Override
		public String getCatalogName() {
			return null;
		}

	}

	/**
	 * Missing constructor.
	 */
	public static final class NoDefaultCtor implements CatalogNameGenerator {

		private NoDefaultCtor() {
		}

		@Override
		public String getCatalogName() {
			return null;
		}

	}

	/**
	 * Throws up all the time.
	 */
	public static class BellyUp implements CatalogNameGenerator {

		public BellyUp() {
			throw new IllegalArgumentException("Ooops");
		}

		@Override
		public String getCatalogName() {
			return null;
		}

	}

	/**
	 * Completely derailed.
	 */
	public static class WrongType {

	}

}
