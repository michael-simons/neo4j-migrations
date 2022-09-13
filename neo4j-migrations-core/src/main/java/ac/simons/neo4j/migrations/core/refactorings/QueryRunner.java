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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.Objects;

import org.neo4j.driver.Query;
import org.neo4j.driver.Result;

/**
 * A simplification over a standard {@see org.neo4j.driver.QueryRunner}. This query runner here is also autocloseable and
 * can and should be treated as such.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
public interface QueryRunner extends AutoCloseable {

	/**
	 * The minimum required version is 4.4 by default.
	 *
	 * @return The default feature set
	 */
	static FeatureSet defaultFeatureSet() {
		return new FeatureSet(false, false, "4.4");
	}

	/**
	 * Runs the given query and returns the result. The result should be consumed.
	 *
	 * @param query The query to run
	 * @return A consumable result.
	 */
	Result run(Query query);

	/**
	 * Defaults to a NOOP.
	 */
	@Override
	default void close() {
	}

	/**
	 * Defines a feature set for any newly opened query runner. Instances of this class are immutable, use the
	 * return type pf any wither-method.
	 */
	final class FeatureSet {

		private final String requiredVersion;

		private final boolean hasBatchingSupport;

		private final boolean hasElementIdSupport;

		private FeatureSet(boolean hasBatchingSupport, boolean hasElementIdSupport,
			String requiredVersion) {
			this.hasBatchingSupport = hasBatchingSupport;
			this.hasElementIdSupport = hasElementIdSupport;
			this.requiredVersion = requiredVersion;
		}

		/**
		 * @return The required version.
		 */
		public String requiredVersion() {
			return requiredVersion;
		}

		/**
		 * @return {@literal true} when the refactoring requires batching support
		 */
		public boolean hasBatchingSupport() {
			return hasBatchingSupport;
		}

		/**
		 * @return {@literal true} when the refactoring makes use of {@code elementId()}
		 */
		public boolean hasElementIdSupport() {
			return hasElementIdSupport;
		}

		/**
		 * Configures batching support.
		 *
		 * @param batchingSupport Set to {@literal true} for requesting batching support
		 * @return New feature set
		 */
		public FeatureSet withBatchingSupport(boolean batchingSupport) {
			return this.hasBatchingSupport == batchingSupport ? this :
				new FeatureSet(batchingSupport, this.hasElementIdSupport, this.requiredVersion);
		}

		/**
		 * Configures elementId support.
		 *
		 * @param elementIdSupport Set to {@literal true} for requesting elementId support
		 * @return New feature set
		 */
		public FeatureSet withElementIdSupport(boolean elementIdSupport) {
			return this.hasElementIdSupport == elementIdSupport ? this :
				new FeatureSet(this.hasBatchingSupport, elementIdSupport, this.requiredVersion);
		}

		/**
		 * Configure the minimum required version.
		 *
		 * @param newRequiredVersion The new required minimum version
		 * @return New feature set
		 */
		public FeatureSet withRequiredVersion(String newRequiredVersion) {

			return Objects.equals(this.requiredVersion, newRequiredVersion) ? this :
				new FeatureSet(this.hasBatchingSupport, this.hasElementIdSupport, newRequiredVersion);
		}
	}
}
