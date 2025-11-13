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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.ChainTool.Pair;
import ac.simons.neo4j.migrations.core.MigrationChain.Element;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings({ "squid:S2187" }) // Sonar doesn't realize that there are tests
final class ChainToolTests {

	private ChainToolTests() {
	}

	public static final Collector<Element, ?, Map<MigrationVersion, Element>> ELEMENT_COLLECTOR = Collectors
		.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity());

	private static final Element P_1 = pendingMigration("01", "E_1vNew");

	private static final Element P_2 = pendingMigration("02", "x");

	private static final Element P_3 = pendingMigration("03", "x");

	private static final Element A_1 = appliedMigration("01", "E_1vOld");

	private static final Element A_2 = appliedMigration("02", "y");

	private static final Element A_3 = appliedMigration("03", "x");

	private static final ConnectionDetails DEFAULT_CONNECTION_DETAILS = new DefaultConnectionDetails("n/a", "n/a",
			"n/a", "n/a", "n/a", "n/a");

	static MigrationChain.Element pendingMigration(String version, String checksum) {
		var migration = mock(AbstractCypherBasedMigration.class);
		given(migration.getVersion()).willReturn(MigrationVersion.withValue(version));
		given(migration.getChecksum()).willReturn(Optional.ofNullable(checksum));
		given(migration.getSource()).willReturn(version + ".cypher");
		given(migration.getOptionalDescription()).willReturn(Optional.of("a description"));
		return DefaultMigrationChainElement.pendingElement(migration);
	}

	static MigrationChain.Element appliedMigration(String version, String checksum) {
		var targetMigration = mock(Node.class);
		Map<String, Object> properties = new HashMap<>();
		properties.put("type", "CYPHER");
		properties.put("checksum", checksum);
		properties.put("version", version);
		properties.put("source", "foobar.cypher");
		properties.put("description", "a description");
		given(targetMigration.asMap()).willReturn(properties);

		var relationship = mock(Relationship.class);
		given(relationship.get("at")).willReturn(Values.value(ZonedDateTime.now()));
		given(relationship.get("by")).willReturn(Values.value("Der Mann Panik Panzer"));
		given(relationship.get("connectedAs")).willReturn(Values.value("Danger Dan"));
		given(relationship.get("in")).willReturn(Values.value(Duration.ofSeconds(4711)));
		given(relationship.get("checksum")).willReturn((checksum != null) ? Values.value(checksum) : Values.NULL);

		var pathSegment = mock(Path.Segment.class);
		given(pathSegment.end()).willReturn(targetMigration);
		given(pathSegment.relationship()).willReturn(relationship);
		return DefaultMigrationChainElement.appliedElement(pathSegment, List.of());
	}

	@SuppressWarnings("unchecked")
	@Nested
	class Comparisons {

		private final Method findMissingSourceElements;

		Comparisons() {
			this.findMissingSourceElements = ReflectionUtils.getRequiredMethod(ChainTool.class,
					"findMissingSourceElements");
			this.findMissingSourceElements.setAccessible(true);
		}

		static Stream<Arguments> shouldFindMissingInTarget() {
			return Stream.of(P_1, P_2, P_3).map(Arguments::of);
		}

		@ParameterizedTest
		@MethodSource
		void shouldFindMissingInTarget(Element toRemove) throws InvocationTargetException, IllegalAccessException {

			var sources = new ArrayList<>(List.of(P_1, P_2, P_3));
			sources.remove(toRemove);

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					sources.stream().collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(A_1, A_2, A_3).collect(ELEMENT_COLLECTOR));

			var missing = (Set<MigrationVersion>) this.findMissingSourceElements.invoke(
					new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source, target));
			assertThat(missing).containsExactly(MigrationVersion.withValue(toRemove.getVersion()));
		}

	}

	@SuppressWarnings("unchecked")
	@Nested
	class ShouldBuildCorrectPairs {

		private final Method findPairs;

		ShouldBuildCorrectPairs() {
			this.findPairs = ReflectionUtils.getRequiredMethod(ChainTool.class, "findPairs");
			this.findPairs.setAccessible(true);
		}

		@Test
		void fromSameChains() throws InvocationTargetException, IllegalAccessException {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_2, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(A_1, A_2, A_3).collect(ELEMENT_COLLECTOR));

			var pairs = (Map<MigrationVersion, Pair>) this.findPairs.invoke(
					new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source, target));
			assertThat(pairs).hasSize(3)
				.containsExactly(Map.entry(MigrationVersion.withValue("01"), new Pair(P_1, A_1)),
						Map.entry(MigrationVersion.withValue("02"), new Pair(P_2, A_2)),
						Map.entry(MigrationVersion.withValue("03"), new Pair(P_3, A_3)));
		}

		@Test
		void fromDifferentSource() throws InvocationTargetException, IllegalAccessException {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(A_1, A_2, A_3).collect(ELEMENT_COLLECTOR));

			var pairs = (Map<MigrationVersion, Pair>) this.findPairs.invoke(
					new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source, target));
			assertThat(pairs).hasSize(2)
				.containsExactly(Map.entry(MigrationVersion.withValue("01"), new Pair(P_1, A_1)),
						Map.entry(MigrationVersion.withValue("03"), new Pair(P_3, A_3)));
		}

		@Test
		void fromDifferentTarget() throws InvocationTargetException, IllegalAccessException {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(A_1).collect(ELEMENT_COLLECTOR));

			var pairs = (Map<MigrationVersion, Pair>) this.findPairs.invoke(
					new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source, target));
			assertThat(pairs).containsExactly(Map.entry(MigrationVersion.withValue("01"), new Pair(P_1, A_1)));
		}

	}

	@Nested
	class Pairs {

		@Test
		void shouldDetectSameChecksumsPresent() {
			var e1 = pendingMigration("32", "ABC");
			var e2 = appliedMigration("32", "ABC");
			var pair = new Pair(e1, e2);
			assertThat(pair.checksumDiffers()).isFalse();
		}

		@Test
		void shouldDetectSameChecksumsAbsent() {
			var e1 = pendingMigration("32", null);
			var e2 = appliedMigration("32", null);
			var pair = new Pair(e1, e2);
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
			var pair = new Pair(e1, e2);
			assertThat(pair.checksumDiffers()).isTrue();
		}

	}

	@Nested
	class Checksums {

		@Test
		void shouldGenerateChecksumFixingQueries() {
			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_2, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(A_1, A_2, A_3).collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));
			assertThat(queries).hasSize(2).satisfies(q -> {
				assertThat(q.parameters().get("version").asString()).isEqualTo("01");
				assertThat(q.parameters().get("newChecksum").asString()).isEqualTo("E_1vNew");
				assertThat(q.parameters().get("oldChecksum").asString()).isEqualTo("E_1vOld");
			}, Index.atIndex(0));
		}

	}

	@Nested
	class Missing {

		@Test
		void shouldGenerateDeleteQueries() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_2).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(appliedMigration("02", "x"), A_3).collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));
			assertThat(queries).hasSize(1).first().satisfies(q -> {
				assertThat(q.parameters().get("version").asString()).isEqualTo("03");
			});
		}

	}

	@Nested
	class Additionals {

		@Test
		void shouldInsertAtTheBeginning() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, pendingMigration("01.1", "a"), P_2).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(appliedMigration(P_2.getVersion(), P_2.getChecksum().orElseThrow()))
						.collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));

			assertThat(queries).hasSize(2)
				.satisfies(q -> assertThat(q.parameters().get("version").asString()).isEqualTo("01"), Index.atIndex(0))
				.satisfies(q -> assertThat(q.parameters().get("version").asString()).isEqualTo("01.1"),
						Index.atIndex(1));
		}

		@Test
		void shouldInsertInTheMiddle() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_2, pendingMigration("02.1", "c"), P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream
						.of(appliedMigration(P_1.getVersion(), P_1.getChecksum().orElseThrow()),
								appliedMigration(P_3.getVersion(), P_3.getChecksum().orElseThrow()))
						.collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));

			assertThat(queries).hasSize(2)
				.satisfies(q -> assertThat(q.parameters().get("version").asString()).isEqualTo("02"), Index.atIndex(0))
				.satisfies(q -> assertThat(q.parameters().get("version").asString()).isEqualTo("02.1"),
						Index.atIndex(1));
		}

		@Test
		void shouldNotAppendV1() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, P_2, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream
						.of(appliedMigration(P_1.getVersion(), P_1.getChecksum().orElseThrow()),
								appliedMigration(P_2.getVersion(), P_2.getChecksum().orElseThrow()))
						.collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));

			assertThat(queries).isEmpty();
		}

		@Test
		void shouldNotAppendV2() {

			var source = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream.of(P_1, pendingMigration("01.1", "c"), P_2, P_3).collect(ELEMENT_COLLECTOR));
			var target = new DefaultMigrationChain(DEFAULT_CONNECTION_DETAILS,
					Stream
						.of(appliedMigration(P_1.getVersion(), P_1.getChecksum().orElseThrow()),
								appliedMigration(P_2.getVersion(), P_2.getChecksum().orElseThrow()))
						.collect(ELEMENT_COLLECTOR));

			var chainTool = new ChainTool(MigrationsConfig.defaultConfig().getVersionComparator(), List.of(), source,
					target);
			var config = MigrationsConfig.defaultConfig();
			var queries = chainTool.repair(config, new DefaultMigrationContext(config, mock(Driver.class)));

			assertThat(queries).hasSize(1)
				.first()
				.satisfies(q -> assertThat(q.parameters().get("version").asString()).isEqualTo("01.1"));
		}

	}

}
