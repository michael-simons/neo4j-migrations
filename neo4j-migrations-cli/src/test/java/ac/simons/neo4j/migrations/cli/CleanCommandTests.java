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

import java.util.Collections;

import ac.simons.neo4j.migrations.core.CleanResult;
import ac.simons.neo4j.migrations.core.Migrations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
class CleanCommandTests {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldInvokeClean(boolean all) {

		Migrations migrations = mock(Migrations.class);
		CleanResult result = mock(CleanResult.class);
		given(result.prettyPrint()).willReturn("cleaned");
		given(result.getWarnings()).willReturn(Collections.singletonList("a warning"));
		given(migrations.clean(anyBoolean())).willReturn(result);

		CleanCommand cmd = new CleanCommand();
		ReflectionSupport
			.findFields(CleanCommand.class, f -> f.getName().equals("all"), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, all);
				}
				catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			});

		cmd.withMigrations(migrations);

		verify(migrations).clean(all);
		verify(result).prettyPrint();
		verify(result).getWarnings();

		verifyNoMoreInteractions(migrations, result);
	}

}
