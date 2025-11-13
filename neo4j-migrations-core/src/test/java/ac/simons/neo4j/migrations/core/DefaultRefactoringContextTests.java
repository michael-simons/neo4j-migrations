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
import java.util.Collections;

import ac.simons.neo4j.migrations.core.refactorings.QueryRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.Plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class DefaultRefactoringContextTests {

	@ParameterizedTest
	@ValueSource(strings = { "ProduceResults@neo4j", "ProduceResults@foobar", "produceresults@whatever",
			"ProduceResults", "Produceresults@whatever1-will-be.Que-sera" })
	void shouldIdentifyResultProducingOperator(String value) {
		Plan plan = mock(Plan.class);
		given(plan.operatorType()).willReturn(value);
		Assertions.assertThat(DefaultRefactoringContext.isProduceResultOperator(plan)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ProduceResult@neo4j", "ProduceResults@aab__b", "EmptyResult" })
	void shouldNotIdentifyResultInvalidProducingOperator(String value) {
		Plan plan = mock(Plan.class);
		given(plan.operatorType()).willReturn(value);
		assertThat(DefaultRefactoringContext.isProduceResultOperator(plan)).isFalse();
	}

	@Test
	void shouldIdentifySingleElements() {

		Plan plan = mock(Plan.class);
		given(plan.identifiers()).willReturn(Collections.singletonList("n"));
		given(plan.arguments()).willReturn(Collections.singletonMap("Details", Values.value("n")));
		assertThat(DefaultRefactoringContext.hasSingleElement(plan)).isTrue();
	}

	@Test
	void shouldNotIdentifySingleElementsWithoutDetails() {

		Plan plan = mock(Plan.class);
		given(plan.identifiers()).willReturn(Collections.singletonList("n"));
		assertThat(DefaultRefactoringContext.hasSingleElement(plan)).isFalse();
	}

	@Test
	void shouldIdentifySingleElementsWithMoreThanOneIdentifier() {

		Plan plan = mock(Plan.class);
		given(plan.identifiers()).willReturn(Arrays.asList("a", "n", "c"));
		given(plan.arguments()).willReturn(Collections.singletonMap("Details", Values.value("n")));
		assertThat(DefaultRefactoringContext.hasSingleElement(plan)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "a,b", "NULL" })
	void shouldIdentifyNonSingleElements(String value) {

		Plan plan = mock(Plan.class);

		given(plan.identifiers())
			.willReturn(Arrays.stream(value.split(",")).map(v -> "NULL".equals(v) ? null : v).toList());
		given(plan.arguments()).willReturn(Collections.singletonMap("Details", Values.value("c")));
		assertThat(DefaultRefactoringContext.hasSingleElement(plan)).isFalse();
	}

	@Nested
	class EvaluationOfFeatureSet {

		@Test
		void shouldFailEarlyOnUndefined() {
			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.UNDEFINED);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet();
			assertThatIllegalStateException().isThrownBy(() -> ctx.getQueryRunner(featureSet))
				.withMessage("Cannot apply refactorings with an undefined version");
		}

		@Test
		void latestVersionShouldSupportBatchingWithoutChecks() {

			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.LATEST);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet()
				.withBatchingSupport(true)
				.withRequiredVersion("4.4.9");

			assertThatNoException().isThrownBy(() -> ctx.getQueryRunner(featureSet));
		}

		@Test
		void shouldNotFailWithSupportedVersion() {

			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.V4_4);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet()
				.withBatchingSupport(true)
				.withRequiredVersion("4.4.9");

			assertThatNoException().isThrownBy(() -> ctx.getQueryRunner(featureSet));
		}

		@Test
		void shouldFailWithUnsupportedVersion() {

			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.V4_3);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet()
				.withBatchingSupport(true)
				.withRequiredVersion("4.4.9");

			assertThatIllegalArgumentException().isThrownBy(() -> ctx.getQueryRunner(featureSet))
				.withMessage("Supported version is %s which is below the required value of %s", ctx.getVersion(),
						"4.4");
		}

		@Test
		void shouldFailWithUndefinedRequiredVersion() {

			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.V4_3);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet()
				.withBatchingSupport(true)
				.withRequiredVersion(null);

			assertThatIllegalArgumentException().isThrownBy(() -> ctx.getQueryRunner(featureSet))
				.withMessage("Undefined version requested");
		}

		@Test
		void shouldFailIfBatchingIsNotSupported() {

			DefaultRefactoringContext ctx = new DefaultRefactoringContext(() -> null, Neo4jVersion.V4_3);
			QueryRunner.FeatureSet featureSet = QueryRunner.defaultFeatureSet()
				.withBatchingSupport(true)
				.withRequiredVersion("Neo4j/4.3.1");

			assertThatIllegalArgumentException().isThrownBy(() -> ctx.getQueryRunner(featureSet))
				.withMessage("Batching is supported only with Neo4j >= 4.4");
		}

	}

}
