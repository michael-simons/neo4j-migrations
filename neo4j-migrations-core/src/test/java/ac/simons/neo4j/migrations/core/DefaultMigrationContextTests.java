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
package ac.simons.neo4j.migrations.core;

import java.lang.reflect.Method;
import java.util.Collections;

import ac.simons.neo4j.migrations.core.DefaultMigrationContext.ExtendedResultSummary;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;
import org.powermock.reflect.Whitebox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Michael J. Simons
 */
class DefaultMigrationContextTests {

	@Test
	void shouldCatchNonExistingShowProcedures() {

		MigrationsConfig config = MigrationsConfig.defaultConfig();

		ResultSummary resultSummary = mock(ResultSummary.class);
		ServerInfo serverInfo = mock(ServerInfo.class);
		given(serverInfo.address()).willReturn("whatever");
		given(resultSummary.server()).willReturn(serverInfo);

		ExtendedResultSummary extendedResultSummary = new ExtendedResultSummary(false, "Neo4j/dev", "Enterprise",
				resultSummary);

		Session session = mock(Session.class);
		given(session.run("EXPLAIN CALL dbms.procedures() YIELD name RETURN count(*)"))
			.willThrow(new ClientException(Neo4jCodes.PROCEDURE_NOT_FOUND, "n/a"));
		given(session.executeRead(any())).willReturn(extendedResultSummary);

		Driver driver = mock(Driver.class);
		given(driver.session()).willReturn(session);
		given(driver.session(any(SessionConfig.class))).willReturn(session);

		DefaultMigrationContext ctx = new DefaultMigrationContext(config, driver);
		assertThatNoException().isThrownBy(ctx::getConnectionDetails);

		verify(session).run("EXPLAIN CALL dbms.procedures() YIELD name RETURN count(*)");
	}

	@Test
	void shouldRequireSupportedDriverForImpersonationAndFailOtherwise() {

		MigrationsConfig migrationConfig = MigrationsConfig.builder().withImpersonatedUser("foo").build();

		String fieldName = "WITH_IMPERSONATED_USER";
		Method originalValue = Whitebox.getInternalState(DefaultMigrationContext.class, fieldName);
		try {
			Whitebox.setInternalState(DefaultMigrationContext.class, fieldName, (Method) null);
			assertThatIllegalArgumentException().isThrownBy(() -> new DefaultMigrationContext(migrationConfig, null))
				.withMessage("User impersonation requires a driver that supports `withImpersonatedUser`.");
		}
		finally {
			Whitebox.setInternalState(DefaultMigrationContext.class, fieldName, originalValue);
		}
	}

	@Test
	void shouldCatchReflectiveCallIssues() {

		SessionConfig.Builder sessionConfigBuilder = mock(SessionConfig.Builder.class, Answers.RETURNS_SELF);
		given(sessionConfigBuilder.withImpersonatedUser(anyString())).willThrow(new RuntimeException("Boom"));

		MigrationsConfig migrationConfig = MigrationsConfig.builder().withImpersonatedUser("foo").build();

		try (MockedStatic<SessionConfig> utilities = Mockito.mockStatic(SessionConfig.class)) {
			MigrationContext ctx = new DefaultMigrationContext(migrationConfig, null);
			utilities.when(SessionConfig::builder).thenReturn(sessionConfigBuilder);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(ctx::getSessionConfig)
				.withMessage("Could not impersonate a user on the driver level");
		}
	}

	@Test
	void shouldApplyCustomizer() {

		MigrationContext ctx = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), null);

		SessionConfig config = ctx.getSessionConfig(builder -> builder.withDatabase("aDatabase"));
		assertThat(config.database()).hasValue("aDatabase");
	}

	@Test
	void copyIntoBuilderShouldWork() {

		SessionConfig sessionConfig = SessionConfig.builder()
			.withBookmarks(Collections.emptyList())
			.withDatabase("f")
			.withDefaultAccessMode(AccessMode.READ)
			.withFetchSize(42711)
			.withImpersonatedUser("Helge")
			.build();

		SessionConfig newConfig = DefaultMigrationContext.copyIntoBuilder(sessionConfig).build();
		assertThat(newConfig).isEqualTo(sessionConfig);
	}

}
