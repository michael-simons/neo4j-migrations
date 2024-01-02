/*
 * Copyright 2020-2024 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.RepairmentResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Gerrit Meier
 */
class RepairCommandTest {

	@Test
	void shouldInvokeRepair() {

		Migrations migrations = mock(Migrations.class);
		RepairmentResult result = mock(RepairmentResult.class);
		when(result.prettyPrint()).thenReturn("success");
		when(result.getWarnings()).thenReturn(Collections.singletonList("a warning"));
		when(migrations.repair()).thenReturn(result);

		RepairCommand cmd = new RepairCommand();
		cmd.withMigrations(migrations);

		verify(migrations).repair();
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}
}
