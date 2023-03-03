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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import ac.simons.neo4j.migrations.core.MigrationChain.Element;

/**
 * @author Michael J. Simons
 */
class ChainToolTest {

	private static final Element P_1 = pendingMigration("01", "E_1vNew");
	private static final Element P_2 = pendingMigration("02", "x");
	private static final Element P_3 = pendingMigration("03", "x");
	private static final Element A_1 = appliedMigration("01", "E_1vOld");
	private static final Element A_2 = appliedMigration("02", "y");
	private static final Element A_3 = appliedMigration("03", "x");
	private static final ConnectionDetails DEFAULT_CONNECTION_DETAILS = new DefaultConnectionDetails("n/a", "n/a", "n/a", "n/a", "n/a", "n/a");

	@Nested
	class Comparisons {
		static Stream<Arguments> shouldFindMissingInTarget() {
			return Stream.of(P_1, P_2, P_3).map(Arguments::of);
		}

		@ParameterizedTest
		@MethodSource
		void shouldFindMissingInTarget(Element toRemove) {

			var sources = new ArrayList<>(List.of(P_1, P_2, P_3));
			sources.remove(toRemove);

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, sources.stream().collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(A_1, A_2, A_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));

			var missing = new ChainTool(source, target).findMissingSourceElements();
			assertThat(missing)
				.containsExactly(toRemove.getVersion());
		}
	}

	@Nested
	class ShouldBuildCorrectPairs {

		@Test
		void fromSameChains() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(P_1, P_2, P_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(A_1, A_2, A_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));

			var pairs = new ChainTool(source, target).findPairs();
			assertThat(pairs)
				.hasSize(3)
				.containsExactly(
					Map.entry("01", new ChainTool.Pair(P_1, A_1)),
					Map.entry("02", new ChainTool.Pair(P_2, A_2)),
					Map.entry("03", new ChainTool.Pair(P_3, A_3))
				);
		}

		@Test
		void fromDifferentSource() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(P_1, P_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(A_1, A_2, A_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));

			var pairs = new ChainTool(source, target).findPairs();
			assertThat(pairs)
				.hasSize(2)
				.containsExactly(
					Map.entry("01", new ChainTool.Pair(P_1, A_1)),
					Map.entry("03", new ChainTool.Pair(P_3, A_3))
				);
		}

		@Test
		void fromDifferentTarget() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(P_1, P_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(A_1).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));

			var pairs = new ChainTool(source, target).findPairs();
			assertThat(pairs)
				.containsExactly(Map.entry("01", new ChainTool.Pair(P_1, A_1)));
		}
	}

	@Nested
	class Pairs {

		@Test
		void shouldDetectSameChecksumsPresent() {
			var e1 = pendingMigration("32", "ABC");
			var e2 = appliedMigration("32", "ABC");
			var pair = new ChainTool.Pair(e1, e2);
			assertThat(pair.checksumDiffers()).isFalse();
		}

		@Test
		void shouldDetectSameChecksumsAbsent() {
			var e1 = pendingMigration("32", null);
			var e2 = appliedMigration("32", null);
			var pair = new ChainTool.Pair(e1, e2);
			assertThat(pair.checksumDiffers()).isFalse();
		}

		@ParameterizedTest
		@CsvSource(nullValues = "n", textBlock = """
			A, B
			A, n
			n, B
			""")
		void shouldDetectDifferentChecksums(String source, String target) {
			var e1 = pendingMigration("32", source);
			var e2 = appliedMigration("32", target);
			var pair = new ChainTool.Pair(e1, e2);
			assertThat(pair.checksumDiffers()).isTrue();
		}
	}

	@Nested
	class Checksums {

		@Test
		void shouldGenerateChecksumFixingQueries() {
			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(P_1, P_2, P_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS, Stream.of(A_1, A_2, A_3).collect(Collectors.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity())));

			var chainTool = new ChainTool(source, target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));
			assertThat(queries).hasSize(2)
				.satisfies(q -> {
					assertThat(q.parameters().get("version").asString()).isEqualTo("01");
					assertThat(q.parameters().get("newChecksum").asString()).isEqualTo("E_1vNew");
					assertThat(q.parameters().get("oldChecksum").asString()).isEqualTo("E_1vOld");
				}, Index.atIndex(0))
			;

		}
	}

	static MigrationChain.Element pendingMigration(String version, String checksum) {
		var migration = mock(AbstractCypherBasedMigration.class);
		when(migration.getVersion()).thenReturn(MigrationVersion.withValue(version));
		when(migration.getChecksum()).thenReturn(Optional.ofNullable(checksum));
		return DefaultMigrationChainElement.pendingElement(migration);
	}

	static MigrationChain.Element appliedMigration(String version, String checksum) {
		var targetMigration = mock(Node.class);
		Map<String, Object> properties = new HashMap<>();
		properties.put("type", "CYPHER");
		properties.put("checksum", checksum);
		properties.put("version", version);
		when(targetMigration.asMap()).thenReturn(properties);

		var relationship = mock(Relationship.class);
		when(relationship.get("at")).thenReturn(Values.value(ZonedDateTime.now()));
		when(relationship.get("by")).thenReturn(Values.value("Der Mann Panik Panzer"));
		when(relationship.get("connectedAs")).thenReturn(Values.value("Danger Dan"));
		when(relationship.get("in")).thenReturn(Values.value(Duration.ofSeconds(4711)));
		when(relationship.get("checksum")).thenReturn(checksum == null ? Values.NULL : Values.value(checksum));

		var pathSegment = mock(Path.Segment.class);
		when(pathSegment.end()).thenReturn(targetMigration);
		when(pathSegment.relationship()).thenReturn(relationship);
		return DefaultMigrationChainElement.appliedElement(pathSegment, List.of());
	}
}
