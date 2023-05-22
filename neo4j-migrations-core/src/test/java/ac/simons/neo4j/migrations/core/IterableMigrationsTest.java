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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * @author Michael J. Simons
 */
class IterableMigrationsTest {

	static Stream<Arguments> iteratingWithoutDelayShouldWork() {
		return Stream.of(Arguments.of((Object) null), Arguments.of(Duration.ofSeconds(1)));
	}

	@ParameterizedTest
	@MethodSource
	void iteratingWithoutDelayShouldWork(Duration delay) {

		List<Migration> migrations = List.of(Mockito.mock(JavaBasedMigration.class), Mockito.mock(JavaBasedMigration.class));
		var iterableMigrations = new IterableMigrations(MigrationsConfig.builder().withDelayBetweenMigrations(delay).build(), migrations);
		var start = System.currentTimeMillis();
		int cnt = 0;
		for (@SuppressWarnings("unused") Migration migration : iterableMigrations) {
			++cnt;
		}
		var duration = System.currentTimeMillis() - start;
		assertThat(cnt).isEqualTo(2);
		if (delay == null) {
			assertThat(duration).isLessThan(1_000);
		} else {
			assertThat(duration).isGreaterThanOrEqualTo(delay.multipliedBy(cnt).toSeconds());
		}
	}
}
