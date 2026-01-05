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

import java.util.Map;

import ac.simons.neo4j.migrations.core.refactorings.Counters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Michael J. Simons
 */
class GodotTests {

	@Test
	void shouldNotWaitIfNoIndexesAreCreate() {
		Session runner = mock(Session.class);
		HBD.vladimirAndEstragonMayWait(runner, Counters.empty());
		Mockito.verifyNoInteractions(runner);
	}

	@Test
	void shouldWaitIfIndexesAreCreate() {
		Session runner = mock(Session.class);
		given(runner.run(any(String.class))).willReturn(mock(Result.class));
		HBD.vladimirAndEstragonMayWait(runner, Counters.of(Map.of("indexesAdded", 23)));
		verify(runner).run("CALL db.awaitIndexes()");
	}

	@Test
	void shouldWaitIfConstraintsAreCreate() {
		Session runner = mock(Session.class);
		given(runner.run(any(String.class))).willReturn(mock(Result.class));
		HBD.vladimirAndEstragonMayWait(runner, Counters.of(Map.of("constraintsAdded", 23)));
		verify(runner).run("CALL db.awaitIndexes()");
	}

}
