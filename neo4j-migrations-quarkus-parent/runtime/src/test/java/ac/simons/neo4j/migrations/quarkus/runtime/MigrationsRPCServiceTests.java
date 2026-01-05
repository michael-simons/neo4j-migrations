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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.CleanResult;
import ac.simons.neo4j.migrations.core.ConnectionDetails;
import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
class MigrationsRPCServiceTests {

	@Test
	void getLabelShouldWork() {

		var migrations = mock(Migrations.class);
		given(migrations.getConnectionDetails())
			.willReturn(ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", "adb", "asdb"));

		var service = new MigrationsRPCService(migrations);
		assertThat(service.getLabel()).isEqualTo("theAddress (adb)");

		verify(migrations, times(2)).getConnectionDetails();
		verifyNoMoreInteractions(migrations);
	}

	@Test
	void getLabelShouldWorkWithoutDb() {
		var migrations = mock(Migrations.class);

		given(migrations.getConnectionDetails())
			.willReturn(ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", null, "asdb"));

		var service = new MigrationsRPCService(migrations);
		assertThat(service.getLabel()).isEqualTo("theAddress (default database)");

		verify(migrations, times(2)).getConnectionDetails();
		verifyNoMoreInteractions(migrations);
	}

	@Test
	void getConnectionDetailsWithDbsShouldWork() {
		var migrations = mock(Migrations.class);
		var connectionDetails = ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", "a", "b");
		given(migrations.getConnectionDetails()).willReturn(connectionDetails);

		var service = new MigrationsRPCService(migrations);
		var result = service.getConnectionDetails();
		assertThat(result.getString("serverAddress")).isEqualTo(connectionDetails.getServerAddress());
		assertThat(result.getString("serverVersion")).isEqualTo(connectionDetails.getServerVersion());
		assertThat(result.getString("username")).isEqualTo(connectionDetails.getUsername());
		assertThat(result.getString("database")).isEqualTo("a");
		assertThat(result.getString("schemaDatabase")).isEqualTo("b");

		verify(migrations).getConnectionDetails();
		verifyNoMoreInteractions(migrations);
	}

	@Test
	void getConnectionDetailsWithoutDbsShouldWork() {
		var migrations = mock(Migrations.class);
		var connectionDetails = ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", null, null);
		given(migrations.getConnectionDetails()).willReturn(connectionDetails);

		var service = new MigrationsRPCService(migrations);
		var result = service.getConnectionDetails();
		assertThat(result.getString("serverAddress")).isEqualTo(connectionDetails.getServerAddress());
		assertThat(result.getString("serverVersion")).isEqualTo(connectionDetails.getServerVersion());
		assertThat(result.getString("username")).isEqualTo(connectionDetails.getUsername());
		assertThat(result.containsKey("database")).isFalse();
		assertThat(result.containsKey("schemaDatabase")).isFalse();

		verify(migrations).getConnectionDetails();
		verifyNoMoreInteractions(migrations);
	}

	@Test
	void migrateShouldWork() {
		var migrations = mock(Migrations.class);
		given(migrations.info()).willReturn(MigrationChain
			.empty(ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", null, null)));

		var service = new MigrationsRPCService(migrations);
		assertThatNoException().isThrownBy(service::migrate);

		verify(migrations).info();
		verify(migrations).apply();
		verifyNoMoreInteractions(migrations);
	}

	@Test
	void cleanShouldWork() {
		var migrations = mock(Migrations.class);
		given(migrations.info()).willReturn(MigrationChain
			.empty(ConnectionDetails.of("theAddress", "4711", "USS Stargazer", "Picard", null, null)));
		var cleanResult = mock(CleanResult.class);
		given(cleanResult.prettyPrint()).willReturn("Irrelevant");
		given(migrations.clean(anyBoolean())).willReturn(cleanResult);

		var service = new MigrationsRPCService(migrations);
		assertThatNoException().isThrownBy(service::clean);

		verify(migrations).clean(false);
		verify(migrations).info();
		verify(cleanResult).prettyPrint();
		verifyNoMoreInteractions(migrations, cleanResult);
	}

}
