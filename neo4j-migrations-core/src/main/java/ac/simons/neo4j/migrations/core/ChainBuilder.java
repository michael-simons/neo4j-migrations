/*
 * Copyright 2020-2021 the original author or authors.
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

import ac.simons.neo4j.migrations.core.MigrationChain.Element;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.util.ServerVersion;
import org.neo4j.driver.summary.DatabaseInfo;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

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
	 * @see #buildChain(MigrationContext, List, boolean)
	 */
	MigrationChain buildChain(MigrationContext context, List<Migration> discoveredMigrations) {
		return buildChain(context, discoveredMigrations, false);
	}

	/**
	 * @param context              The current context
	 * @param discoveredMigrations A list of migrations sorted by {@link Migration#getVersion()}.
	 *                             It is not yet known whether those are pending or not.
	 * @param detailedCauses       set to {@literal true} to add causes to possible exceptions
	 * @return The full migration chain.
	 */
	MigrationChain buildChain(MigrationContext context, List<Migration> discoveredMigrations, boolean detailedCauses) {

		final Map<MigrationVersion, Element> elements = buildChain0(context, discoveredMigrations, detailedCauses);

		class ExtendedResultSummary {
			final boolean showCurrentUserExists;
			final ServerVersion version;
			final ServerInfo server;
			final DatabaseInfo database;

			ExtendedResultSummary(boolean showCurrentUserExists, ServerVersion version, ResultSummary actualSummary) {
				this.showCurrentUserExists = showCurrentUserExists;
				this.version = version;
				this.server = actualSummary.server();
				this.database = actualSummary.database();
			}
		}

		try (Session session = context.getSchemaSession()) {

			// Auth maybe disabled. In such cases, we cannot get the current user.
			ExtendedResultSummary databaseInformation = session.readTransaction(tx -> {
				Result result = tx.run(""
					+ "CALL dbms.procedures() YIELD name "
					+ "WHERE name = 'dbms.showCurrentUser' "
					+ "WITH count(*) > 0 AS showCurrentUserExists "
					+ "CALL dbms.components() YIELD versions "
					+ "RETURN showCurrentUserExists, 'Neo4j/' + versions[0] AS version"
				);
				Record singleResultRecord = result.single();
				boolean showCurrentUserExists = singleResultRecord.get("showCurrentUserExists").asBoolean();
				ServerVersion version = ServerVersion.version(singleResultRecord.get("version").asString());
				ResultSummary summary = result.consume();
				return new ExtendedResultSummary(showCurrentUserExists, version, summary);
			});

			String username = "anonymous";
			if (databaseInformation.showCurrentUserExists) {

				username = session.readTransaction(tx -> tx.run(""
					+ "CALL dbms.procedures() YIELD name "
					+ "WHERE name = 'dbms.showCurrentUser' "
					+ "CALL dbms.showCurrentUser() YIELD username RETURN username"
				).single().get("username").asString());
			}

			ServerInfo serverInfo = databaseInformation.server;
			String schemaDatabase = databaseInformation.database == null ? null : databaseInformation.database.name();
			String targetDatabase = context.getConfig().getMigrationTargetIn(context).orElse(schemaDatabase);
			return new DefaultMigrationChain(serverInfo.address(), databaseInformation.version.toString(),
				username, targetDatabase, schemaDatabase, elements);
		}
	}

	private Map<MigrationVersion, Element> buildChain0(MigrationContext context, List<Migration> discoveredMigrations, boolean detailedCauses) {

		Map<MigrationVersion, Element> appliedMigrations = getChainOfAppliedMigrations(context);
		if (discoveredMigrations.isEmpty()) {
			// No migrations found, everything in the chain is applied
			Collections.unmodifiableMap(appliedMigrations);
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
				String message = "More migrations have been applied to the database than locally resolved";
				if (detailedCauses) {
					throw new MigrationsException(message, e);
				}
				throw new MigrationsException(message);
			}
			if (!newMigration.getVersion().equals(expectedVersion)) {
				throw new MigrationsException("Unexpected migration at index " + i + ": " + Migrations.toString(newMigration));
			}

			if ((context.getConfig().isValidateOnMigrate() || alwaysVerify) && !expectedChecksum.equals(newMigration.getChecksum())) {
				throw new MigrationsException("Checksum of " + Migrations.toString(newMigration) + " changed!");
			}
			// This is not a pending migration anymore
			fullMigrationChain.put(expectedVersion, entry.getValue());
			++i;
		}

		// All remaining migrations are pending
		while (i < discoveredMigrations.size()) {
			Migration pendingMigration = discoveredMigrations.get(i++);
			Element element = DefaultChainElement.pendingElement(pendingMigration);
			fullMigrationChain.put(pendingMigration.getVersion(), element);
		}

		return Collections.unmodifiableMap(fullMigrationChain);
	}

	private Map<MigrationVersion, Element> getChainOfAppliedMigrations(MigrationContext context) {

		Map<MigrationVersion, Element> chain = new LinkedHashMap<>();
		try (Session session = context.getSchemaSession()) {
			Result result = session
				.run("MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [r:MIGRATED_TO*] -> (l:__Neo4jMigration) \n"
					+ "WHERE coalesce(b.migrationTarget,'<default>') = coalesce($migrationTarget,'<default>') AND NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration)\n"
					+ "RETURN p", Collections.singletonMap("migrationTarget", context.getConfig().getMigrationTargetIn(context).orElse(null)));
			// Might be empty (when nothing has applied yet)
			if (result.hasNext()) {
				result.single().get("p").asPath().forEach(segment -> {
					Element chainElement = DefaultChainElement.appliedElement(segment);
					chain.put(MigrationVersion.withValue(chainElement.getVersion()), chainElement);
				});
			}
		}
		return chain;
	}

	private static class DefaultMigrationChain implements MigrationChain {

		private final String serverAdress;

		private final String serverVersion;

		private final String username;

		private final String databaseName;

		private final String schemaDatabaseName;

		private final Map<MigrationVersion, Element> elements;

		DefaultMigrationChain(String serverAdress, String serverVersion, String username, String databaseName, String schemaDatabaseName,
			Map<MigrationVersion, Element> elements) {
			this.serverAdress = serverAdress;
			this.serverVersion = serverVersion;
			this.username = username;
			this.databaseName = databaseName;
			this.schemaDatabaseName = schemaDatabaseName;
			this.elements = elements;
		}

		@Override
		public String getServerAddress() {
			return serverAdress;
		}

		@Override
		public String getServerVersion() {
			return serverVersion;
		}

		@Override public String getUsername() {
			return username;
		}

		@SuppressWarnings("deprecation")
		@Override
		public String getDatabaseName() {
			return databaseName;
		}

		@Override
		public Optional<String> getOptionalDatabaseName() {
			return Optional.ofNullable(databaseName);
		}

		@Override
		public Optional<String> getOptionalSchemaDatabaseName() {
			return Optional.ofNullable(schemaDatabaseName);
		}

		@Override
		public boolean isApplied(String version) {
			Element element = this.elements.get(MigrationVersion.withValue(version));
			return element != null && element.getState() == MigrationState.APPLIED;
		}

		@Override
		public Collection<Element> getElements() {
			return this.elements.values();
		}
	}

	private static class DefaultChainElement implements Element {

		static class InstallationInfo {

			private final ZonedDateTime installedOn;

			private final String installedBy;

			private final Duration executionTime;

			InstallationInfo(ZonedDateTime installedOn, String installedBy, Duration executionTime) {
				this.installedOn = installedOn;
				this.installedBy = installedBy;
				this.executionTime = executionTime;
			}

			ZonedDateTime getInstalledOn() {
				return installedOn;
			}

			String getInstalledBy() {
				return installedBy;
			}

			Duration getExecutionTime() {
				return executionTime;
			}
		}

		static Element appliedElement(Path.Segment appliedMigration) {

			Node targetMigration = appliedMigration.end();
			Map<String, Object> properties = targetMigration.asMap();

			Relationship migrationProperties = appliedMigration.relationship();
			ZonedDateTime installedOn = migrationProperties.get("at").asZonedDateTime();
			String installedBy = String.format("%s/%s", migrationProperties.get("by").asString(),
				migrationProperties.get("connectedAs").asString());
			IsoDuration storedExecutionTime = migrationProperties.get("in").asIsoDuration();
			Duration executionTime = Duration.ofSeconds(storedExecutionTime.seconds())
				.plusNanos(storedExecutionTime.nanoseconds());

			return new DefaultChainElement(MigrationState.APPLIED,
				MigrationType.valueOf((String) properties.get("type")), (String) properties.get("checksum"),
				(String) properties.get("version"), (String) properties.get("description"),
				(String) properties.get("source"), new InstallationInfo(installedOn, installedBy, executionTime));
		}

		static Element pendingElement(Migration pendingMigration) {
			return new DefaultChainElement(MigrationState.PENDING, Migrations.getMigrationType(pendingMigration),
				pendingMigration.getChecksum().orElse(null), pendingMigration.getVersion().getValue(),
				pendingMigration.getDescription(), pendingMigration.getSource(), null);
		}

		private final MigrationState state;

		private final MigrationType type;

		private final String checksum;

		private final String version;

		private final String description;

		private final String source;

		private final InstallationInfo installationInfo;

		private DefaultChainElement(MigrationState state, MigrationType type, String checksum,
			String version, String description, String source, InstallationInfo installationInfo) {
			this.state = state;
			this.type = type;
			this.checksum = checksum;
			this.version = version;
			this.description = description;
			this.source = source;
			this.installationInfo = installationInfo;
		}

		@Override
		public MigrationState getState() {
			return state;
		}

		@Override
		public MigrationType getType() {
			return type;
		}

		@Override
		public Optional<String> getChecksum() {
			return Optional.ofNullable(checksum);
		}

		@Override
		public String getVersion() {
			return version;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getSource() {
			return source;
		}

		@Override
		public Optional<ZonedDateTime> getInstalledOn() {
			return Optional.ofNullable(installationInfo).map(InstallationInfo::getInstalledOn);
		}

		@Override
		public Optional<String> getInstalledBy() {
			return Optional.ofNullable(installationInfo).map(InstallationInfo::getInstalledBy);
		}

		@Override
		public Optional<Duration> getExecutionTime() {
			return Optional.ofNullable(installationInfo).map(InstallationInfo::getExecutionTime);
		}
	}
}
