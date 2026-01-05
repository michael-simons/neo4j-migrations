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
package ac.simons.neo4j.migrations.cli;

import java.util.Collections;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
  */
class ValidateCommandTests {

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2 })
	void shouldInvokeValidate(int levelOfFailure) {

		boolean makeItFail = levelOfFailure > 0;

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		given(result.prettyPrint()).willReturn("things have been validated");
		if (makeItFail) {
			given(result.getWarnings()).willReturn(Collections.singletonList("a warning"));
			given(result.isValid()).willReturn(false);
			if (levelOfFailure == 2) {
				given(result.needsRepair()).willReturn(true);
			}
		}
		else {
			given(result.isValid()).willReturn(true);
		}
		given(migrations.validate()).willReturn(result);

		ValidateCommand cmd = new ValidateCommand();

		int exitCode = cmd.withMigrations(migrations);
		assertThat(exitCode).isEqualTo(makeItFail ? 1 : 0);

		verify(migrations).validate();
		verify(result).isValid();
		verify(result).prettyPrint();
		if (makeItFail) {
			verify(result).needsRepair();
			verify(result).getWarnings();
		}

		verifyNoMoreInteractions(migrations, result);
	}

}
