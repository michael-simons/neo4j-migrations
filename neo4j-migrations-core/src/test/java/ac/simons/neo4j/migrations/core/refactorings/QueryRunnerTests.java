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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class QueryRunnerTests {

	@Test
	void defaultFeatureSetShouldBeSane() {
		QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet();
		assertThat(featureSet.hasBatchingSupport()).isFalse();
		assertThat(featureSet.hasElementIdSupport()).isFalse();
		assertThat(featureSet.requiredVersion()).isEqualTo("4.4");
	}

	@Nested
	class QueryRunnerFeatureSet {

		@Test
		void withBatchingSupportShouldWork() {

			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet();
			assertThat(featureSet.withBatchingSupport(featureSet.hasBatchingSupport())).isSameAs(featureSet);

			boolean newValue = !featureSet.hasBatchingSupport();
			QueryRunner.FeatureSet newFeatureSet = featureSet.withBatchingSupport(newValue);
			assertThat(newFeatureSet).isNotSameAs(featureSet);
			assertThat(newFeatureSet.hasBatchingSupport()).isEqualTo(newValue);
		}

		@Test
		void withElementIdSupportShouldWork() {

			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet();
			assertThat(featureSet.withElementIdSupport(featureSet.hasElementIdSupport())).isSameAs(featureSet);

			boolean newValue = !featureSet.hasElementIdSupport();
			QueryRunner.FeatureSet newFeatureSet = featureSet.withElementIdSupport(newValue);
			assertThat(newFeatureSet).isNotSameAs(featureSet);
			assertThat(newFeatureSet.hasElementIdSupport()).isEqualTo(newValue);
		}

		@Test
		void withRequiredVersionShouldWork() {

			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet();
			assertThat(featureSet.withRequiredVersion(featureSet.requiredVersion())).isSameAs(featureSet);

			String newValue = "whatever";
			QueryRunner.FeatureSet newFeatureSet = featureSet.withRequiredVersion(newValue);
			assertThat(newFeatureSet).isNotSameAs(featureSet);
			assertThat(newFeatureSet.requiredVersion()).isEqualTo(newValue);
		}

	}

}
