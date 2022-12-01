/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import ac.simons.neo4j.migrations.core.MigrationChain.ChainBuilderMode;
import ac.simons.neo4j.migrations.core.MigrationChain.Element;

/**
 * Builder for retrieving information about a database and creating a chain containing applied and pending migrations.
 *
 * @author Michael J. Simons
 * @soundtrack Kettcar - Ich vs. wir
 * @since 0.0.4
 */
final class ChainBuilder {

	/**
	 * A flag to force the chain builder into verification mode.
	 */
	private final boolean alwaysVerify;

	ChainBuilder() {
		this(false);
	}

	ChainBuilder(boolean alwaysVerify) {
		this.alwaysVerify = alwaysVerify;
	}

	/**
	 * @param context              The current context
	 * @param discoveredMigrations A list of migrations sorted by {@link Migration#getVersion()}.
	 *                             It is not yet known whether those are pending or not.
	 * @return The full migration chain.
	 * @see #buildChain(MigrationContext, List, boolean, ChainBuilderMode)
	 */
	MigrationChain buildChain(MigrationContext context, List<Migration> discoveredMigrations) {
		return buildChain(context, discoveredMigrations, false, ChainBuilderMode.COMPARE);
	}

	/**
	 * @param context              The current context
	 * @param discoveredMigrations A list of migrations sorted by {@link Migration#getVersion()}.
	 *                             It is not yet known whether those are pending or not.
	 * @param detailedCauses       set to {@literal true} to add causes to possible exceptions
	 * @return The full migration chain.
	 */
	MigrationChain buildChain(MigrationContext context, List<Migration> discoveredMigrations, boolean detailedCauses, ChainBuilderMode mode) {

		final Map<MigrationVersion, Element> elements = buildChain0(context, discoveredMigrations, detailedCauses, mode);
		return new DefaultMigrationChain(context.getConnectionDetails(), elements);
	}

	@SuppressWarnings("squid:S3776") // Yep, this is a complex validation, but it still fits on one screen
	private Map<MigrationVersion, Element> buildChain0(MigrationContext context, List<Migration> discoveredMigrations, boolean detailedCauses, ChainBuilderMode mode) {

		Map<MigrationVersion, Element> appliedMigrations =
			mode == ChainBuilderMode.LOCAL ? Collections.emptyMap() : getChainOfAppliedMigrations(context);
		if (mode == ChainBuilderMode.REMOTE) {
			// Only looking at remote, assume everything is applied
			return Collections.unmodifiableMap(appliedMigrations);
		}

		Map<MigrationVersion, Element> fullMigrationChain = new LinkedHashMap<>(
			discoveredMigrations.size() + appliedMigrations.size());
		int i = 0;
		for (Map.Entry<MigrationVersion, Element> entry : appliedMigrations.entrySet()) {
			MigrationVersion expectedVersion = entry.getKey();
			Optional<String> expectedChecksum = entry.getValue().getChecksum();

			Migration newMigration;
			try {
				newMigration = discoveredMigrations.get(i);
			} catch (IndexOutOfBoundsException e) {
				String message = "More migrations have been applied to the database than locally resolved.";
				if (detailedCauses) {
					throw new MigrationsException(message, e);
				}
				throw new MigrationsException(message);
			}

			if (!newMigration.getVersion().equals(expectedVersion)) {
				throw new MigrationsException("Unexpected migration at index " + i + ": " + Migrations.toString(newMigration) + ".");
			}

			if (newMigration.isRepeatable() != expectedVersion.isRepeatable()) {
				throw new MigrationsException("State of " + Migrations.toString(newMigration) + " changed from " + (expectedVersion.isRepeatable() ? "repeatable to non-repeatable" : "non-repeatable to repeatable"));
			}

			if ((context.getConfig().isValidateOnMigrate() || alwaysVerify) && !(matches(expectedChecksum, newMigration) || expectedVersion.isRepeatable())) {
				throw new MigrationsException("Checksum of " + Migrations.toString(newMigration) + " changed!");
			}

			// This is not a pending migration anymore
			fullMigrationChain.put(expectedVersion, entry.getValue());
			++i;
		}

		// All remaining migrations are pending
		while (i < discoveredMigrations.size()) {
			Migration pendingMigration = discoveredMigrations.get(i++);
			Element element = DefaultMigrationChainElement.pendingElement(pendingMigration);
			fullMigrationChain.put(pendingMigration.getVersion(), element);
		}

		return Collections.unmodifiableMap(fullMigrationChain);
	}

	static boolean matches(Optional<String> expectedChecksum, Migration newMigration) {

		if (expectedChecksum.equals(newMigration.getChecksum())) {
			return true;
		}

		if (!(newMigration instanceof MigrationWithPreconditions) || !expectedChecksum.isPresent()) {
			return false;
		}

		return ((MigrationWithPreconditions) newMigration).getAlternativeChecksums().contains(expectedChecksum.get());
	}

	private Map<MigrationVersion, Element> getChainOfAppliedMigrations(MigrationContext context) {

		var query = """
			MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [r:MIGRATED_TO*] -> (l:__Neo4jMigration)
			WHERE coalesce(b.migrationTarget,'<default>') = coalesce($migrationTarget,'<default>') AND NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration)
			WITH p
			OPTIONAL MATCH () - [r:REPEATED] -> ()
			WITH p, r order by r.at DESC
			RETURN p, collect(r) AS repetitions
			""";

		try (Session session = context.getSchemaSession()) {
			return session.executeRead(tx -> {
				Map<MigrationVersion, Element> chain = new LinkedHashMap<>();
				String migrationTarget = context.getConfig().getMigrationTargetIn(context).orElse(null);
				Result result = tx.run(query, Collections.singletonMap("migrationTarget", migrationTarget));
				// Might be empty (when nothing has applied yet)
				if (result.hasNext()) {
					Record row = result.single();
					List<Relationship> repetitions = row.get("repetitions").asList(Value::asRelationship);
					row.get("p").asPath().forEach(segment -> {
						Element chainElement = DefaultMigrationChainElement.appliedElement(segment, repetitions);
						chain.put(MigrationVersion.withValue(chainElement.getVersion(), segment.end().get("repeatable").asBoolean(false)), chainElement);
					});
				}
				return chain;
			});
		}
	}
}
