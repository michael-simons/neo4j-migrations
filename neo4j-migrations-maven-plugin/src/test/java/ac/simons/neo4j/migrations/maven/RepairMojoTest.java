/*
 * Copyright 2020-2023 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.RepairmentResult;
import org.junit.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Gerrit Meier
 */
public class RepairMojoTest {

	@Test
	public void shouldInvokeRepair() throws Exception {

		Migrations migrations = mock(Migrations.class);
		RepairmentResult result = mock(RepairmentResult.class);
		when(result.prettyPrint()).thenReturn("repaired things");
		when(migrations.repair()).thenReturn(result);

		String log = tapSystemErr(() -> {
			RepairMojo mojo = new RepairMojo();
			mojo.withMigrations(migrations);
		});

		assertThat(log).contains("repaired things");

		verify(migrations).repair();
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}

}
