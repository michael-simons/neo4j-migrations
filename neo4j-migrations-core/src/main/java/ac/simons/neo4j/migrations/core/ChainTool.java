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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 * This class is responsible for create a diff of two {@link MigrationChain migration chain instances}. The diff is then
 * used to make one chain identical to the other.
 * <p>
 * The chains are not labeled left and right but source and  target on purpose: The ultimate goal is to make target look
 * like source, so the outcome of the diff gives also an information if the desired state is possible to achieve.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class ChainTool {

	record Pair(MigrationChain.Element e1, MigrationChain.Element e2) {

		boolean checksumDiffers() {
			return !e1.getChecksum().equals(e2.getChecksum());
		}

		boolean hasChecksums() {
			return e1.getChecksum().isPresent() && e2.getChecksum().isPresent();
		}
	}

	private final MigrationChain source;

	private final Map<String, MigrationChain.Element> sourceElements;

	private final MigrationChain target;

	private final Map<String, MigrationChain.Element> targetElements;

	ChainTool(MigrationChain source, MigrationChain target) {
		this.source = source;
		this.target = target;

		this.sourceElements = source.getElements().stream()
			.collect(Collectors.toMap(MigrationChain.Element::getVersion, Function.identity()));
		this.targetElements = target.getElements().stream()
			.collect(Collectors.toMap(MigrationChain.Element::getVersion, Function.identity()));
	}

	List<Query> repair(MigrationsConfig config, MigrationContext context) {

		var pairs = findPairs();
		var migrationTarget = config.getMigrationTargetIn(context).orElse(null);

		List<Query> result = new ArrayList<>(fixChecksums(migrationTarget, pairs));
		result.addAll(deleteMigration(migrationTarget));
		return result;
	}

	static Query generateMigrationDeletionQuery(String migrationTarget, String version) {

		String cypher = """
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

		return new Query(cypher,
			Values.parameters(
				Migrations.PROPERTY_MIGRATION_VERSION, version,
				Migrations.PROPERTY_MIGRATION_TARGET, migrationTarget
			)
		);
	}

	private List<Query> fixChecksums(String migrationTarget, Map<String, Pair> pairs) {

		String cypher = """        
				MATCH (m:__Neo4jMigration {version: $version, checksum: $oldChecksum})
				WHERE coalesce(m.migrationTarget, '<default>') = coalesce($migrationTarget,'<default>')
				SET m.checksum = $newChecksum
			""";

		Predicate<Pair> withChecksumFilter = Pair::checksumDiffers;
		withChecksumFilter = withChecksumFilter.and(Pair::hasChecksums);

		return pairs.values().stream()
			.filter(withChecksumFilter)
			.map(p -> new Query(cypher,
					Values.parameters(
						Migrations.PROPERTY_MIGRATION_VERSION, p.e1.getVersion(),
						"oldChecksum", p.e2.getChecksum().orElseThrow(),
						"newChecksum", p.e1.getChecksum().orElseThrow(),
						Migrations.PROPERTY_MIGRATION_TARGET, migrationTarget
					)
				)
			)
			.toList();
	}

	private List<Query> deleteMigration(String migrationTarget) {

		return findMissingSourceElements()
			.stream()
			.map(version -> generateMigrationDeletionQuery(migrationTarget, version))
			.toList();
	}

	Map<String, Pair> findPairs() {

		var sharedKeys = new HashSet<>(sourceElements.keySet());
		sharedKeys.retainAll(targetElements.keySet());
		return sharedKeys.stream()
			.collect(Collectors.toMap(
				Function.identity(),
				k -> new Pair(sourceElements.get(k), targetElements.get(k)),
				(o, n) -> {
					throw new IllegalStateException("Must not contain duplicate keys");
				},
				TreeMap::new
			));
	}

	List<String> findMissingSourceElements() {

		return targetElements.keySet().stream().filter(element -> !sourceElements.containsKey(element))
			.toList();
	}
}
