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
package ac.simons.neo4j.migrations.core;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class IterableMigrationsTests {

	static Stream<Arguments> iteratingWithoutDelayShouldWork() {
		return Stream.of(Arguments.of((Object) null), Arguments.of(Duration.ofSeconds(1)));
	}

	static List<Migration> mockMigrations(String firstVersion, String... moreVersions) {
		return Stream.concat(Stream.of(firstVersion), Stream.of(moreVersions))
			.map(v -> MigrationVersion.parse(v + "__na"))
			.map(v -> {
				Migration m = mock(JavaBasedMigration.class);
				given(m.getVersion()).willReturn(v);
				return m;
			})
			.toList();
	}

	static Stream<Arguments> stopVersions() {
		return Stream.of(Arguments.of("V023_1_1", List.of("10", "20")), Arguments.of("V5", List.of()),
				Arguments.of("V50", List.of("10", "20", "30", "40", "50")),
				Arguments.of("V60", List.of("10", "20", "30", "40", "50", "60")),
				Arguments.of("V61", List.of("10", "20", "30", "40", "50", "60")));
	}

	@ParameterizedTest
	@MethodSource
	void iteratingWithoutDelayShouldWork(Duration delay) {

		List<Migration> migrations = List.of(mock(JavaBasedMigration.class), mock(JavaBasedMigration.class));
		var iterableMigrations = IterableMigrations
			.of(MigrationsConfig.builder().withDelayBetweenMigrations(delay).build(), migrations);
		var start = System.currentTimeMillis();
		int cnt = 0;
		for (@SuppressWarnings("unused")
		Migration migration : iterableMigrations) {
			++cnt;
		}
		var duration = System.currentTimeMillis() - start;
		assertThat(cnt).isEqualTo(2);
		if (delay == null) {
			assertThat(duration).isLessThan(1_000);
		}
		else {
			assertThat(duration).isGreaterThanOrEqualTo(delay.multipliedBy(cnt).toSeconds());
		}
	}

	@ParameterizedTest // GH-1536
	@ValueSource(booleans = { true, false })
	void optionalStopVersionShouldBeChecked(boolean optional) {

		var migrations = mockMigrations("V10");
		var config = MigrationsConfig.defaultConfig();
		var stopVersion = new MigrationVersion.StopVersion(
				MigrationVersion.parse("V2025__Is going to be so much fun.cypher"), optional);

		ThrowingCallable iterableMigrationsSupplier = () -> IterableMigrations.of(config, migrations, stopVersion);
		if (optional) {
			assertThatNoException().isThrownBy(iterableMigrationsSupplier);
		}
		else {
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(iterableMigrationsSupplier)
				.withMessage("Target version 2025 is not available");
		}
	}

	@ParameterizedTest // GH-1536
	@MethodSource
	void stopVersions(String version, List<String> expected) {

		var migrations = mockMigrations("V10", "R20", "V30", "V40", "R50", "V60");
		var config = MigrationsConfig.builder()
			.withVersionSortOrder(MigrationsConfig.VersionSortOrder.SEMANTIC)
			.build();
		var stopVersion = new MigrationVersion.StopVersion(MigrationVersion.parse(version + "__na"), true);

		assertThat(IterableMigrations.of(config, migrations, stopVersion)).extracting(Migration::getVersion)
			.extracting(MigrationVersion::getValue)
			.containsExactlyElementsOf(expected);

	}

	@Test // GH-1536
	void shouldThrowIfDone() {

		var it = IterableMigrations.of(MigrationsConfig.defaultConfig(), mockMigrations("V1")).iterator();
		while (it.hasNext()) {
			assertThat(it.next()).isNotNull();
		}
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(it::next);
	}

}
