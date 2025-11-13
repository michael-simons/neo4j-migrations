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

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.MigrateBTreeIndexes;
import picocli.CommandLine;

/**
 * A hidden command for accessing the {@link MigrateBTreeIndexes} refactoring directly.
 *
 * @author Michael J. Simons
 * @since 1.15.0
 */
@CommandLine.Command(name = "migrateBTreeIndexes",
		description = "Migrates existing B-tree indexes and constraints backed by such indexes to Neo4j 5.0+ and higher supported indexes. By default, new indexes will be created in parallel with a suffix attached to their name.",
		hidden = true)
public class MigrateBTreeIndexesCommand extends ConnectedCommand {

	@CommandLine.Option(names = "--mapping",
			description = "Use the given type for the index or constraint with the matching name.\nSpecify multiple times for multiple mappings.")
	Map<String, IndexType> mappings;

	@CommandLine.ParentCommand
	private MigrationsCli parent;

	@CommandLine.Option(names = "suffix", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
			defaultValue = MigrateBTreeIndexes.DEFAULT_SUFFIX,
			description = "The suffix for the new indexes created in parallel.")
	private String suffix;

	@CommandLine.Option(names = "replace", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
			defaultValue = "false",
			description = "Drops existing B-tree indexes and constraints and creates new one with the old names.")
	private boolean replace;

	@CommandLine.Option(names = "--exclude",
			description = "Names of indexes and constraints to exclude in migration.\nSpecify multiple times for multiple excludes.")
	private Set<String> excludes;

	@CommandLine.Option(names = "--include",
			description = "Names of indexes and constraints to include in migration.\nSpecify multiple times for multiple excludes.")
	private Set<String> includes;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	boolean forceSilence() {
		return true;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		if (this.excludes != null) {
			MigrationsCli.LOGGER.log(Level.INFO, "Excluding the following schema items: {0}",
					new Object[] { String.join(", ", this.excludes) });
		}

		if (this.includes != null) {
			MigrationsCli.LOGGER.log(Level.INFO, "Including the following schema items: {0}",
					new Object[] { String.join(", ", this.includes) });
		}

		Map<String, Index.Type> typeMapping = null;
		if (this.mappings != null) {
			typeMapping = this.mappings.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> switch (e.getValue()) {
					case RANGE -> Index.Type.PROPERTY;
					case POINT -> Index.Type.POINT;
					case TEXT -> Index.Type.TEXT;
				}));
			String readableMappings = this.mappings.entrySet()
				.stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(System.lineSeparator()));
			MigrationsCli.LOGGER.log(Level.INFO, "Using the following mapping:{0}{1}",
					new Object[] { System.lineSeparator(), readableMappings });
		}

		MigrateBTreeIndexes refactoring;
		if (this.replace) {
			refactoring = MigrateBTreeIndexes.replaceBTreeIndexes();
		}
		else {
			refactoring = MigrateBTreeIndexes.createFutureIndexes(this.suffix);
		}
		refactoring = refactoring.withTypeMapping(typeMapping).withIncludes(this.includes).withExcludes(this.excludes);
		Counters counters = migrations.apply(refactoring);
		if (this.replace) {
			MigrationsCli.LOGGER.log(Level.INFO,
					"Deleted {0} BTree based indexes and {1} constraints and replaced them with {2} new indexes and {3} constraints.",
					new Object[] { counters.indexesRemoved(), counters.constraintsRemoved(), counters.indexesAdded(),
							counters.constraintsAdded() });
		}
		else {
			MigrationsCli.LOGGER.log(Level.INFO, "Added {0} new indexes and {1} constraints.",
					new Object[] { counters.indexesAdded(), counters.constraintsAdded() });
		}
		return 0;
	}

	enum IndexType {

		RANGE, TEXT, POINT

	}

}
