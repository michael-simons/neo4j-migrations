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
package ac.simons.neo4j.migrations.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.ValidationResult;

import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @soundtrack Sugarcubes - Life's Too Good
 */
class ValidateCommandTest {

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2 })
	void shouldInvokeValidate(int levelOfFailure) {

		boolean makeItFail = levelOfFailure > 0;

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		when(result.prettyPrint()).thenReturn("things have been validated");
		if (makeItFail) {
			when(result.getWarnings()).thenReturn(Collections.singletonList("a warning"));
			when(result.isValid()).thenReturn(false);
			if (levelOfFailure == 2) {
				when(result.needsRepair()).thenReturn(true);
			}
		} else {
			when(result.isValid()).thenReturn(true);
		}
		when(migrations.validate()).thenReturn(result);

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
