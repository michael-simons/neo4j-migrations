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

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class ValidationResultTests {

	@ParameterizedTest
	@CsvSource(textBlock = """
			VALID,true
			INCOMPLETE_DATABASE,false
			INCOMPLETE_MIGRATIONS,false
			DIFFERENT_CONTENT,false
			""")
	void isValidShouldWork(ValidationResult.Outcome outcome, boolean expected) {
		ValidationResult result = new ValidationResult(Optional.empty(), outcome, Collections.emptyList());
		assertThat(result.getOutcome()).isEqualTo(outcome);
		assertThat(result.isValid()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			VALID,false
			INCOMPLETE_DATABASE,false
			INCOMPLETE_MIGRATIONS,true
			DIFFERENT_CONTENT,true
			""")
	void needsRepairShouldWork(ValidationResult.Outcome outcome, boolean expected) {
		ValidationResult result = new ValidationResult(Optional.empty(), outcome, Collections.emptyList());
		assertThat(result.getOutcome()).isEqualTo(outcome);
		assertThat(result.needsRepair()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(value = { "aDatabase", "n/a" }, nullValues = "n/a")
	void getAffectedDatabaseShouldWork(String affectedDatabase) {

		assertThat(new ValidationResult(Optional.ofNullable(affectedDatabase), ValidationResult.Outcome.VALID,
				Collections.emptyList())
			.getAffectedDatabase()).isEqualTo(Optional.ofNullable(affectedDatabase));
	}

	@ParameterizedTest
	@CsvSource(nullValues = "n/a", textBlock = """
				VALID,aDatabase,All resolved migrations have been applied to `aDatabase`.
				VALID,n/a,All resolved migrations have been applied to the default database.
				INCOMPLETE_MIGRATIONS,kaputt,Some versions previously applied to `kaputt` cannot be resolved anymore.
			""")
	void prettyPrintingShouldWork(ValidationResult.Outcome outcome, String affectedDatabase, String expected) {

		ValidationResult result = new ValidationResult(Optional.ofNullable(affectedDatabase), outcome,
				Collections.emptyList());
		assertThat(result.prettyPrint()).isEqualTo(expected);
	}

}
