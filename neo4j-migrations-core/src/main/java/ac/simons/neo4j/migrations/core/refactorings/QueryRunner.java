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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.Objects;

import org.neo4j.driver.Query;
import org.neo4j.driver.Result;

/**
 * A simplification over a standard {@see org.neo4j.driver.QueryRunner}. This query runner
 * here is also autocloseable and can and should be treated as such.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
public interface QueryRunner extends AutoCloseable {

	/**
	 * The minimum required version is 4.4 by default.
	 * @return the default feature set
	 */
	static FeatureSet defaultFeatureSet() {
		return new FeatureSet(false, false, "4.4");
	}

	/**
	 * Runs the given query and returns the result. The result should be consumed.
	 * @param query the query to run
	 * @return a consumable result.
	 */
	Result run(Query query);

	/**
	 * Defaults to a NOOP.
	 */
	@Override
	default void close() {
	}

	/**
	 * Defines a feature set for any newly opened query runner. Instances of this class
	 * are immutable, use the return type pf any wither-method.
	 */
	final class FeatureSet {

		private final String requiredVersion;

		private final boolean hasBatchingSupport;

		private final boolean hasElementIdSupport;

		private FeatureSet(boolean hasBatchingSupport, boolean hasElementIdSupport, String requiredVersion) {
			this.hasBatchingSupport = hasBatchingSupport;
			this.hasElementIdSupport = hasElementIdSupport;
			this.requiredVersion = requiredVersion;
		}

		/**
		 * {@return the required version}
		 */
		public String requiredVersion() {
			return this.requiredVersion;
		}

		/**
		 * {@return <code>true</code> when the refactoring requires batching support}
		 */
		public boolean hasBatchingSupport() {
			return this.hasBatchingSupport;
		}

		/**
		 * {@return <code>true</code> when the refactoring makes use of elementId()}
		 */
		public boolean hasElementIdSupport() {
			return this.hasElementIdSupport;
		}

		/**
		 * Configures batching support.
		 * @param batchingSupport set to <code>true</code> for requesting batching support
		 * @return new feature set
		 */
		public FeatureSet withBatchingSupport(boolean batchingSupport) {
			return (this.hasBatchingSupport == batchingSupport) ? this
					: new FeatureSet(batchingSupport, this.hasElementIdSupport, this.requiredVersion);
		}

		/**
		 * Configures elementId support.
		 * @param elementIdSupport set to <code>true</code> for requesting elementId
		 * support
		 * @return new feature set
		 */
		public FeatureSet withElementIdSupport(boolean elementIdSupport) {
			return (this.hasElementIdSupport == elementIdSupport) ? this
					: new FeatureSet(this.hasBatchingSupport, elementIdSupport, this.requiredVersion);
		}

		/**
		 * Configure the minimum required version.
		 * @param newRequiredVersion the new required minimum version
		 * @return new feature set
		 */
		public FeatureSet withRequiredVersion(String newRequiredVersion) {

			return Objects.equals(this.requiredVersion, newRequiredVersion) ? this
					: new FeatureSet(this.hasBatchingSupport, this.hasElementIdSupport, newRequiredVersion);
		}

	}

}
