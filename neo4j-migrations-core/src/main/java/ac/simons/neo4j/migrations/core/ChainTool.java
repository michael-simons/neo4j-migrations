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

	private final MigrationChain source;

	private final MigrationChain target;


	record Pair(MigrationChain.Element e1, MigrationChain.Element e2) {

		boolean checksumDiffers() {
			return !e1.getChecksum().equals(e2.getChecksum());
		}

		boolean hasChecksums() {
			return e1.getChecksum().isPresent() && e2.getChecksum().isPresent();
		}
	}

	ChainTool(MigrationChain source, MigrationChain target) {
		this.source = source;
		this.target = target;
	}

	List<Query> repair(MigrationsConfig config, MigrationContext context) {
		var pairs = findPairs(source, target);
		var migrationTarget = config.getMigrationTargetIn(context).orElse(null);
		if (pairs.size() == source.length() && source.length() == target.length()) {
			return fixChecksums(migrationTarget, pairs);
		}
		return List.of();
	}

	private List<Query> fixChecksums(String migrationTarget, Map<String, Pair> pairs) {

		String query = """        
				MATCH (m:__Neo4jMigration {version: $version, checksum: $oldChecksum})
				WHERE coalesce(m.migrationTarget, '<default>') = coalesce($migrationTarget,'<default>')
				SET m.checksum = $newChecksum
			""";

		Predicate<Pair> withChecksumFilter = Pair::checksumDiffers;
		withChecksumFilter = withChecksumFilter.and(Pair::hasChecksums);

		return pairs.values().stream()
			.filter(withChecksumFilter)
			.map(p -> new Query(query,
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

	static Map<String, Pair> findPairs(MigrationChain source, MigrationChain target) {

		var sourceElements = source.getElements().stream()
			.collect(Collectors.toMap(MigrationChain.Element::getVersion, Function.identity()));
		var targetElements = target.getElements().stream()
			.collect(Collectors.toMap(MigrationChain.Element::getVersion, Function.identity()));

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
}
