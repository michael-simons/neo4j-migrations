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
package ac.simons.neo4j.migrations.maven;

import java.lang.reflect.Field;
import java.util.Collections;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.ValidationResult;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
public class ValidateMojoTests {

	@Test
	public void shouldLogOnlyOnValid() throws Exception {

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		given(result.prettyPrint()).willReturn("things have been validated");
		given(migrations.validate()).willReturn(result);
		given(result.isValid()).willReturn(true);

		String log = tapSystemErr(() -> {
			ValidateMojo mojo = new ValidateMojo();
			mojo.withMigrations(migrations);
		});

		assertThat(log).contains("things have been validated");

		verify(migrations).validate();
		verify(result).isValid();
		verify(result).prettyPrint();

		verifyNoMoreInteractions(migrations, result);
	}

	@Test
	public void shouldThrowOnInvalidRepairableByDefault() throws Exception {

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		given(result.prettyPrint()).willReturn("things have been validated");
		given(migrations.validate()).willReturn(result);
		given(result.isValid()).willReturn(false);
		given(result.needsRepair()).willReturn(false);
		given(result.getWarnings()).willReturn(Collections.singletonList("a warning"));

		ValidateMojo mojo = new ValidateMojo();
		setAlwaysFail(mojo, true);

		assertThatExceptionOfType(MojoFailureException.class).isThrownBy(() -> mojo.withMigrations(migrations))
			.satisfies(e -> {
				assertThat(e.getMessage())
					.isEqualTo("Database is not in a valid state. To fix this, apply this configuration.");
				assertThat(e.getLongMessage())
					.isEqualTo("things have been validated" + System.lineSeparator() + "a warning");
			});

		verify(migrations).validate();
		verify(result).isValid();
		verify(result).needsRepair();
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}

	private void setAlwaysFail(ValidateMojo mojo, boolean b) throws NoSuchFieldException, IllegalAccessException {
		// Maven magic sets this to true normally
		Field alwaysFail = ValidateMojo.class.getDeclaredField("alwaysFail");
		alwaysFail.setAccessible(true);
		alwaysFail.set(mojo, b);
	}

	@Test
	public void shouldNotThrowOnNoRepairNeededWhenNotAlwaysFail() throws Exception {

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		given(result.prettyPrint()).willReturn("things have been validated");
		given(migrations.validate()).willReturn(result);
		given(result.isValid()).willReturn(false);
		given(result.needsRepair()).willReturn(false);
		given(result.getWarnings()).willReturn(Collections.singletonList("a warning"));

		ValidateMojo mojo = new ValidateMojo();

		setAlwaysFail(mojo, false);

		String log = tapSystemErr(() -> {
			mojo.withMigrations(migrations);
		});

		assertThat(log).contains("Database is not in a valid state. To fix this, apply this configuration.",
				"things have been validated", "a warning");

		verify(migrations).validate();
		verify(result).isValid();
		verify(result).needsRepair();
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}

	@Test
	public void shouldAlwaysThrowWhenRepairIsNeeded() throws Exception {

		Migrations migrations = mock(Migrations.class);
		ValidationResult result = mock(ValidationResult.class);
		given(result.prettyPrint()).willReturn("things have been validated");
		given(migrations.validate()).willReturn(result);
		given(result.isValid()).willReturn(false);
		given(result.needsRepair()).willReturn(true);
		given(result.getWarnings()).willReturn(Collections.singletonList("a warning"));

		ValidateMojo mojo = new ValidateMojo();

		setAlwaysFail(mojo, false);

		assertThatExceptionOfType(MojoFailureException.class).isThrownBy(() -> mojo.withMigrations(migrations))
			.satisfies(e -> {
				assertThat(e.getMessage()).isEqualTo("Database is not in a valid state and needs manual repair.");
				assertThat(e.getLongMessage())
					.isEqualTo("things have been validated" + System.lineSeparator() + "a warning");
			});

		verify(migrations).validate();
		verify(result).isValid();
		verify(result).needsRepair();
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}

}
