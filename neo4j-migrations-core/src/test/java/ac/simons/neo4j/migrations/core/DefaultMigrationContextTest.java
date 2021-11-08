/*
 * Copyright 2020-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.neo4j.driver.SessionConfig;
import org.powermock.reflect.Whitebox;

/**
 * @author Michael J. Simons
 * @soundtrack Ten Masked Men - The Phanten Masked Menace
 */
class DefaultMigrationContextTest {

	@Test
	void noSuchMethod() {

		MigrationsConfig migrationConfig = MigrationsConfig.builder().withImpersonatedUser("foo").build();

		String fieldName = "WITH_IMPERSONATED_USER";
		Method originalValue = Whitebox.getInternalState(Migrations.DefaultMigrationContext.class, fieldName);
		try {
			Whitebox.setInternalState(Migrations.DefaultMigrationContext.class, fieldName, (Method) null);
			assertThatIllegalArgumentException()
				.isThrownBy(() -> new Migrations.DefaultMigrationContext(migrationConfig, null))
				.withMessage("User impersonation requires a driver that supports `withImpersonatedUser`.");
		} finally {
			Whitebox.setInternalState(Migrations.DefaultMigrationContext.class, fieldName, originalValue);
		}
	}

	@Test
	void shouldCatchReflectiveCallIssues() {

		SessionConfig.Builder sessionConfigBuilder = mock(SessionConfig.Builder.class, Answers.RETURNS_SELF);
		when(sessionConfigBuilder.withImpersonatedUser(anyString())).thenThrow(new RuntimeException("Boom"));

		MigrationsConfig migrationConfig = MigrationsConfig.builder().withImpersonatedUser("foo").build();

		try (MockedStatic<SessionConfig> utilities = Mockito.mockStatic(SessionConfig.class)) {

			utilities.when(SessionConfig::builder).thenReturn(sessionConfigBuilder);
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> new Migrations.DefaultMigrationContext(migrationConfig, null))
				.withMessage("Could not impersonate a user on the driver level");
		}
	}
}
