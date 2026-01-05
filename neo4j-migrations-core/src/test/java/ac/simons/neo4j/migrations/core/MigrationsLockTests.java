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

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings({ "squid:S2187" }) // Sonar doesn't realize that there are tests
class MigrationsLockTests {

	@Nested
	class SummaryCountersImplTest {

		static Stream<Arguments> containsSystemUpdatesShouldWork() {
			return Stream.of(Arguments.of(Integer.MIN_VALUE, false), Arguments.of(-1, false), Arguments.of(0, false),
					Arguments.of(ThreadLocalRandom.current().nextInt(1, 1000), true),
					Arguments.of(Integer.MAX_VALUE, true));
		}

		static Stream<Arguments> containsUpdateShouldWork() {
			var withAPositiveValue = ThreadLocalRandom.current().ints(12, 0, 1000).toArray();
			withAPositiveValue[ThreadLocalRandom.current().nextInt(0, 11)] = 42;
			var onlySysUpdates = new int[12];
			onlySysUpdates[11] = 23;
			var oneUpdateOnly = new int[12];
			oneUpdateOnly[3] = 23;

			return Stream.of(Arguments.of(withAPositiveValue, true), Arguments.of(oneUpdateOnly, true),
					Arguments.of(onlySysUpdates, false));
		}

		@ParameterizedTest
		@MethodSource
		void containsSystemUpdatesShouldWork(int value, boolean expected) {
			var random = ThreadLocalRandom.current();
			var counters = new MigrationsLock.SummaryCountersImpl(random.nextInt(), random.nextInt(), random.nextInt(),
					random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt(),
					random.nextInt(), random.nextInt(), random.nextInt(), value);
			assertThat(counters.containsSystemUpdates()).isEqualTo(expected);
		}

		@Test
		void containsUpdateShouldWorkWithoutUpdates() {
			var counters = new MigrationsLock.SummaryCountersImpl(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			assertThat(counters.containsUpdates()).isFalse();
		}

		@ParameterizedTest
		@MethodSource
		void containsUpdateShouldWork(int[] values, boolean expected) {
			var counters = new MigrationsLock.SummaryCountersImpl(values[0], values[1], values[2], values[3], values[4],
					values[5], values[6], values[7], values[8], values[9], values[10], values[11]);
			assertThat(counters.containsUpdates()).isEqualTo(expected);
		}

	}

}
