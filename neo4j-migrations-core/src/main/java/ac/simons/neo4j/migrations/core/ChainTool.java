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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 * This class is responsible for create a diff of two {@link MigrationChain migration
 * chain instances}. The diff is then used to make one chain identical to the other.
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
final class ChainTool {

	private final Comparator<MigrationVersion> versionComparator;

	/**
	 * The actual locally underlying migrations, not only the chain.
	 */
	private final Map<MigrationVersion, Migration> discoveredMigrations;

	private final MigrationChain newChain;

	private final Map<MigrationVersion, MigrationChain.Element> newElements;

	private final Map<MigrationVersion, MigrationChain.Element> currentElements;

	ChainTool(Comparator<MigrationVersion> versionComparator, Collection<Migration> discoveredMigrations,
			MigrationChain newChain, MigrationChain currentChain) {
		this.versionComparator = versionComparator;
		this.newChain = newChain;
		this.discoveredMigrations = discoveredMigrations.stream()
			.collect(Collectors.toMap(Migration::getVersion, Function.identity()));

		Collector<MigrationChain.Element, ?, TreeMap<MigrationVersion, MigrationChain.Element>> collector = Collectors
			.toMap(e -> MigrationVersion.withValue(e.getVersion()), Function.identity(), throwingMerger(),
					() -> new TreeMap<>(this.versionComparator));

		this.newElements = newChain.getElements().stream().collect(collector);
		this.currentElements = currentChain.getElements().stream().collect(collector);
	}

	static Query generateMigrationDeletionQuery(String migrationTarget, MigrationVersion version) {

		var cypher = """
				MATCH (p)-[l:MIGRATED_TO]->(m:__Neo4jMigration {version: $version})
				OPTIONAL MATCH (m)-[r:MIGRATED_TO]->(n:__Neo4jMigration)
				WHERE coalesce(m.migrationTarget, '<default>') = coalesce($migrationTarget,'<default>')
				WITH p, l, m, r, n
				FOREACH (i in CASE WHEN n IS NOT NULL THEN [1] ELSE [] END |
				  CREATE (p)-[nl:MIGRATED_TO]->(n)
				  SET nl = properties(l)
				)
				WITH l, m, r, properties(m) as p
				DELETE l, m, r
				RETURN p
				""";

		return new Query(cypher, Values.parameters(Migrations.PROPERTY_MIGRATION_VERSION, version.getValue(),
				Migrations.PROPERTY_MIGRATION_TARGET, migrationTarget));
	}

	private static <T> BinaryOperator<T> throwingMerger() {
		return (k, v) -> {
			throw new IllegalStateException("Must not contain duplicate keys");
		};
	}

	List<Query> repair(MigrationsConfig config, MigrationContext context) {

		if (this.currentElements.isEmpty()) {
			return List.of();
		}

		var migrationTarget = config.getMigrationTargetIn(context).orElse(null);

		List<Query> result = new ArrayList<>(fixChecksums(migrationTarget));
		result.addAll(deleteLocallyMissingMigrations(migrationTarget));
		result.addAll(insertRemoteMissingMigrations(migrationTarget, config.getOptionalInstalledBy()));
		return result;
	}

	private List<Query> fixChecksums(String migrationTarget) {

		var cypher = """
				MATCH (m:__Neo4jMigration {version: $version, checksum: $oldChecksum})
				WHERE coalesce(m.migrationTarget, '<default>') = coalesce($migrationTarget,'<default>')
				SET m.checksum = $newChecksum
				""";

		Predicate<Pair> withChecksumFilter = Pair::checksumDiffers;
		withChecksumFilter = withChecksumFilter.and(Pair::hasChecksums);

		return findPairs().values()
			.stream()
			.filter(withChecksumFilter)
			.map(p -> new Query(cypher,
					Values.parameters(Migrations.PROPERTY_MIGRATION_TARGET, migrationTarget,
							Migrations.PROPERTY_MIGRATION_VERSION, p.e1.getVersion(), "oldChecksum",
							p.e2.getChecksum().orElseThrow(), "newChecksum", p.e1.getChecksum().orElseThrow())))
			.toList();
	}

	private List<Query> deleteLocallyMissingMigrations(String migrationTarget) {

		return findMissingSourceElements().stream()
			.map(version -> generateMigrationDeletionQuery(migrationTarget, version))
			.toList();
	}

	private List<Query> insertRemoteMissingMigrations(String migrationTarget, Optional<String> installedBy) {

		var cypher = """
				MATCH (pm:__Neo4jMigration {version: $previousVersion}) - [pr:MIGRATED_TO] -> (nm:__Neo4jMigration)
				WHERE coalesce(pm.migrationTarget, '<default>') = coalesce($migrationTarget,'<default>')
				AND coalesce(pm.migrationTarget, '<default>') = coalesce(nm.migrationTarget, '<default>')
				CREATE (im:__Neo4jMigration {version: $version})
				CREATE (pm)-[nl:MIGRATED_TO {at: datetime({timezone: 'UTC'}), in: duration( {milliseconds: 0} ), by: $installedBy, connectedAs: $neo4jUser}]-> (im)
				CREATE (im)-[nr:MIGRATED_TO]->(nm)
				SET im = $insertedMigration,  nr = properties(pr)
				DELETE pr
				RETURN *
				""";

		var lastKnownVersion = this.currentElements.keySet()
			.stream()
			.skip(this.currentElements.size() - 1L)
			.findFirst()
			.orElseThrow();

		var allVersions = new TreeSet<>(this.versionComparator);
		allVersions.add(MigrationVersion.baseline());
		allVersions.addAll(this.newElements.keySet());
		allVersions.addAll(this.currentElements.keySet());
		allVersions.removeAll(findMissingSourceElements());

		var sortedVersions = new ArrayList<>(allVersions);

		return this.newElements.entrySet()
			.stream()
			.takeWhile(entry -> this.versionComparator.compare(entry.getKey(), lastKnownVersion) < 0)
			.filter(entry -> !this.currentElements.containsKey(entry.getKey()))
			.map(entry -> {
				var migration = entry.getValue();
				var properties = new HashMap<String, Object>();

				properties.put(Migrations.PROPERTY_MIGRATION_VERSION, entry.getKey().getValue());
				migration.getOptionalDescription()
					.ifPresent(v -> properties.put(Migrations.PROPERTY_MIGRATION_DESCRIPTION, v));
				properties.put("type", migration.getType().name());
				properties.put("repeatable",
						Optional.ofNullable(this.discoveredMigrations.get(entry.getKey()))
							.map(Migration::isRepeatable)
							.orElse(false));
				properties.put("source", migration.getSource());
				migration.getChecksum().ifPresent(v -> properties.put("checksum", v));

				return new Query(cypher,
						Values.parameters(Migrations.PROPERTY_MIGRATION_TARGET, migrationTarget,
								Migrations.PROPERTY_MIGRATION_VERSION, entry.getKey().getValue(), "previousVersion",
								sortedVersions.get(sortedVersions.indexOf(entry.getKey()) - 1).getValue(),
								"installedBy", installedBy.map(Values::value).orElse(Values.NULL), "neo4jUser",
								this.newChain.getUsername(), "insertedMigration", properties));
			})
			.toList();
	}

	private Map<MigrationVersion, Pair> findPairs() {

		var sharedKeys = new HashSet<>(this.newElements.keySet());
		sharedKeys.retainAll(this.currentElements.keySet());
		return sharedKeys.stream()
			.collect(Collectors.toMap(Function.identity(),
					k -> new Pair(this.newElements.get(k), this.currentElements.get(k)), throwingMerger(),
					() -> new TreeMap<>(this.versionComparator)));
	}

	private Set<MigrationVersion> findMissingSourceElements() {

		return this.currentElements.keySet()
			.stream()
			.filter(element -> !this.newElements.containsKey(element))
			.collect(Collectors.toSet());
	}

	record Pair(MigrationChain.Element e1, MigrationChain.Element e2) {

		boolean checksumDiffers() {
			return !this.e1.getChecksum().equals(this.e2.getChecksum());
		}

		boolean hasChecksums() {
			return this.e1.getChecksum().isPresent() && this.e2.getChecksum().isPresent();
		}
	}

}
