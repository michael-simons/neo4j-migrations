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

import ac.simons.neo4j.migrations.core.ValidationResult.Outcome;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;

/**
 * Main entry to Neo4j Migrations
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class Migrations {

	private static final Logger LOGGER = Logger.getLogger(Migrations.class.getName());

	private static final String PROPERTY_MIGRATION_TARGET = "migrationTarget";

	private final MigrationsConfig config;
	private final Driver driver;
	private final MigrationContext context;
	private final DiscoveryService discoveryService;
	private final ChainBuilder chainBuilder;
	/**
	 * As the callbacks are used in many phases of the lifecycle of a migration object,
	 * they are computed upfront.
	 */
	private final Map<LifecyclePhase, List<Callback>> callbacks;
	private final AtomicBoolean beforeFirstUseHasBeenCalled = new AtomicBoolean(false);

	/**
	 * Creates a {@link Migrations migrations instance} ready to used with the given configuration over the connection
	 * defined by the {@link Driver driver}.
	 *
	 * @param config The configuration to use
	 * @param driver The connection
	 */
	public Migrations(MigrationsConfig config, Driver driver) {

		this.config = config;
		this.driver = driver;

		this.discoveryService = new DiscoveryService();
		this.chainBuilder = new ChainBuilder();

		this.context = new DefaultMigrationContext(this.config, this.driver);
		this.callbacks = this.discoveryService.findCallbacks(this.context);
	}

	/**
	 * Returns information about the context, the database, all applied and all pending applications.
	 *
	 * @return The chain of migrations.
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @since 0.0.4
	 */
	public MigrationChain info() {

		return executeWithinLock(() -> {
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			return chainBuilder.buildChain(context, migrations);
		}, LifecyclePhase.BEFORE_INFO, LifecyclePhase.AFTER_INFO);
	}

	/**
	 * Applies all discovered Neo4j migrations. Migrations can either be classes implementing {@link JavaBasedMigration}
	 * or Cypher script migrations that are on the classpath or filesystem.
	 *
	 * @return The last applied migration (if any)
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @since 0.0.1
	 */
	public Optional<MigrationVersion> apply() {

		return executeWithinLock(() -> {
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			apply0(migrations);
			return getLastAppliedVersion();
		}, LifecyclePhase.BEFORE_MIGRATE, LifecyclePhase.AFTER_MIGRATE);
	}

	/**
	 * Cleans the {@link MigrationsConfig#getOptionalSchemaDatabase() selected schema database}. If there is no schema
	 * database selected, looks in the {@link MigrationsConfig#getOptionalDatabase() target database.} If this isn't
	 * configured as well, the users home database will be searched for
	 * <ol>
	 * <li>Migration chains (those are the nodes containing information about the applied migrations</li>
	 * <li>Any log from this tool</li>
	 * <li>Any constraints created by this tool</li>
	 * </ol>
	 * and will delete and drop them in that order. This is a <strong>destructive</strong> operation, so make sure not
	 * to apply it to your production database without thinking at least twice. It cannot be undone via Neo4j-Migrations.
	 *
	 * @param all Set to {@literal true} to remove all constructs created by Neo4j-Migrations, set to {@literal false} to
	 *            remove all the migration chain for the selected or automatically determined target database.
	 * @return The result of cleaning the database.
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused due to schema objects not deletable
	 * @since 1.1.0
	 */
	public CleanResult clean(boolean all) {

		Optional<String> optionalMigrationTarget = config.getMigrationTargetIn(context);
		DeletedChainsWithCounters deletedChainsWithCounters
			= executeWithinLock(() -> clean0(optionalMigrationTarget, all), LifecyclePhase.BEFORE_CLEAN, LifecyclePhase.AFTER_CLEAN);

		long nodesDeleted = deletedChainsWithCounters.counter.nodesDeleted();
		long relationshipsDeleted = deletedChainsWithCounters.counter.relationshipsDeleted();
		long constraintsRemoved = 0;
		long indexesRemoved = 0;
		if (all) {
			SummaryCounters additionalCounters = new MigrationsLock(context).clean();
			nodesDeleted += additionalCounters.nodesDeleted();
			relationshipsDeleted += additionalCounters.relationshipsDeleted();
			constraintsRemoved += additionalCounters.constraintsRemoved();
			indexesRemoved += additionalCounters.indexesRemoved();
		}

		return new CleanResult(config.getOptionalSchemaDatabase(), deletedChainsWithCounters.chainsDeleted, nodesDeleted,
			relationshipsDeleted,
			constraintsRemoved, indexesRemoved);
	}

	static class DeletedChainsWithCounters {

		final List<String> chainsDeleted;
		final SummaryCounters counter;

		DeletedChainsWithCounters(List<String> chainsDeleted, SummaryCounters counter) {
			this.chainsDeleted = chainsDeleted;
			this.counter = counter;
		}
	}

	private DeletedChainsWithCounters clean0(
		@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> migrationTarget,
		boolean all
	) {

		String query = ""
			+ "MATCH (n:__Neo4jMigration) "
			+ "WITH n, coalesce(n.migrationTarget, '<default>') as migrationTarget "
			+ "WHERE (migrationTarget = coalesce($migrationTarget,'<default>') OR $all)"
			+ "DETACH DELETE n "
			+ "RETURN DISTINCT migrationTarget "
			+ "ORDER BY migrationTarget ASC ";

		try (Session session = context.getSchemaSession()) {
			Result result = session.run(query, Values.parameters(
				PROPERTY_MIGRATION_TARGET, migrationTarget.orElse(null),
				"all", all));

			return new DeletedChainsWithCounters(
				result.list(r -> r.get(PROPERTY_MIGRATION_TARGET).asString()),
				result.consume().counters()
			);
		}
	}

	/**
	 * Validates the database against the resolved migrations. A database is considered to be in a valid state when all
	 * resolved migrations have been applied (there are no more pending migrations). If a database is not yet fully migrated,
	 * it won't identify as {@link ValidationResult.Outcome#VALID} but it will indicate via {@link ValidationResult#needsRepair()} that
	 * it doesn't need repair. Applying the pending migrations via {@link #apply()} will bring the database into a valid state.
	 * Most other outcomes not valid need to be manually fix. One radical fix is calling {@link Migrations#clean(boolean)}
	 * with the same configuration.
	 *
	 * @return a validation result, with an outcome, a possible list of warnings and indicators if the database is in a valid state
	 * @since 1.2.0
	 */
	public ValidationResult validate() {

		return executeWithinLock(() -> {
			List<Migration> migrations = discoveryService.findMigrations(this.context);
			Optional<String> targetDatabase = config.getOptionalSchemaDatabase();
			try {
				MigrationChain migrationChain = new ChainBuilder(true).buildChain(context, migrations);
				int numberOfAppliedMigrations = (int) migrationChain.getElements()
					.stream().filter(m -> m.getState() == MigrationState.APPLIED)
					.count();
				if (migrations.size() == numberOfAppliedMigrations) {
					return new ValidationResult(targetDatabase, Outcome.VALID, numberOfAppliedMigrations == 0 ?
						Collections.singletonList("No migrations resolved.") :
						Collections.emptyList());
				} else if (migrations.size() > numberOfAppliedMigrations) {
					return new ValidationResult(targetDatabase, Outcome.INCOMPLETE_DATABASE, Collections.emptyList());
				}
				return new ValidationResult(targetDatabase, Outcome.UNDEFINED, Collections.emptyList());
			} catch (MigrationsException e) {
				List<String> warnings = Collections.singletonList(e.getMessage());
				if (e.getCause() instanceof IndexOutOfBoundsException) {
					return new ValidationResult(targetDatabase, Outcome.INCOMPLETE_MIGRATIONS, warnings);
				}
				return new ValidationResult(targetDatabase, Outcome.DIFFERENT_CONTENT, warnings);
			}
		}, LifecyclePhase.BEFORE_VALIDATE, LifecyclePhase.AFTER_VALIDATE);
	}

	/**
	 * @return the user agent for Neo4j-Migrations (given in the form {@literal name/version}).
	 * @since 1.2.1
	 */
	public static String getUserAgent() {
		return "neo4j-migrations/" + ProductionVersion.getValue();
	}

	private <T> T executeWithinLock(Supplier<T> executable, LifecyclePhase before, LifecyclePhase after) {

		driver.verifyConnectivity();

		MigrationsLock lock = new MigrationsLock(this.context);
		try {
			lock.lock();
			if (beforeFirstUseHasBeenCalled.compareAndSet(false, true)) {
				invokeCallbacks(LifecyclePhase.BEFORE_FIRST_USE);
			}
			try {
				invokeCallbacks(before);
				return executable.get();
			} finally {
				invokeCallbacks(after);
			}
		} finally {
			lock.unlock();
		}
	}

	private void invokeCallbacks(LifecyclePhase phase) {

		LifecycleEvent event = new DefaultLifecycleEvent(phase, this.context);
		this.callbacks.getOrDefault(phase, Collections.emptyList())
			.forEach(callback -> callback.on(event));
	}

	private Optional<MigrationVersion> getLastAppliedVersion() {

		try (Session session = context.getSchemaSession()) {
			Node lastMigration = session.readTransaction(tx ->
				tx.run(
					"MATCH (l:__Neo4jMigration) WHERE coalesce(l.migrationTarget,'<default>') = coalesce($migrationTarget,'<default>') AND NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) RETURN l",
						Collections.singletonMap(PROPERTY_MIGRATION_TARGET, config.getMigrationTargetIn(context).orElse(null)))
				.single().get(0).asNode());

			String version = lastMigration.get("version").asString();
			String description = lastMigration.get("description").asString();

			return Optional.of(MigrationVersion.withValueAndDescription(version, description));
		} catch (NoSuchRecordException e) {
			return Optional.empty();
		}
	}

	private void apply0(List<Migration> migrations) {

		MigrationVersion previousVersion = getLastAppliedVersion()
			.orElseGet(MigrationVersion::baseline);

		// Validate and build the chain of migrations
		MigrationChain chain = chainBuilder.buildChain(context, migrations);

		StopWatch stopWatch = new StopWatch();
		for (Migration migration : migrations) {

			if (previousVersion != MigrationVersion.baseline() && chain.isApplied(migration.getVersion().getValue())) {
				LOGGER.log(Level.INFO, "Skipping already applied migration {0}", toString(migration));
				continue;
			}
			try {
				stopWatch.start();
				migration.apply(context);
				long executionTime = stopWatch.stop();
				previousVersion = recordApplication(chain.getUsername(), previousVersion, migration, executionTime);

				LOGGER.log(Level.INFO, "Applied migration {0}", toString(migration));
			} catch (Exception e) {
				throw new MigrationsException("Could not apply migration: " + toString(migration), e);
			} finally {
				stopWatch.reset();
			}
		}
	}

	private MigrationVersion recordApplication(String neo4jUser, MigrationVersion previousVersion, Migration appliedMigration,
		long executionTime) {

		try (Session session = context.getSchemaSession()) {

			Optional<String> migrationTarget = context.getConfig().getMigrationTargetIn(context);
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("neo4jUser", neo4jUser);
			parameters.put("previousVersion", previousVersion.getValue());
			parameters.put("appliedMigration", toProperties(appliedMigration));
			parameters.put("installedBy", config.getOptionalInstalledBy().map(Values::value).orElse(Values.NULL));
			parameters.put("executionTime", executionTime);
			parameters.put(PROPERTY_MIGRATION_TARGET, migrationTarget.orElse(null));

			session.writeTransaction(t -> {
				String mergeOrMatchAndMaybeCreate;
				if (migrationTarget.isPresent()) {
					mergeOrMatchAndMaybeCreate = "MERGE (p:__Neo4jMigration {version: $previousVersion, migrationTarget: $migrationTarget}) ";
				} else {
					Result result = t.run(
						"MATCH (p:__Neo4jMigration {version: $previousVersion}) WHERE p.migrationTarget IS NULL RETURN id(p) AS id",
						Values.parameters("previousVersion", previousVersion.getValue()));
					if (result.hasNext()) {
						parameters.put("id", result.single().get("id").asLong());
						mergeOrMatchAndMaybeCreate = "MATCH (p) WHERE id(p) = $id WITH p ";
					} else {
						mergeOrMatchAndMaybeCreate = "CREATE (p:__Neo4jMigration {version: $previousVersion}) ";
					}
				}

				return t.run(
						mergeOrMatchAndMaybeCreate
							+ "CREATE (c:__Neo4jMigration) SET c = $appliedMigration, c.migrationTarget = $migrationTarget "
							+ "MERGE (p) - [:MIGRATED_TO {at: datetime({timezone: 'UTC'}), in: duration( {milliseconds: $executionTime} ), by: $installedBy, connectedAs: $neo4jUser}] -> (c)",
						parameters)
					.consume();
			});
		}

		return appliedMigration.getVersion();
	}

	private static Map<String, Object> toProperties(Migration migration) {

		Map<String, Object> properties = new HashMap<>();

		properties.put("version", migration.getVersion().getValue());
		properties.put("description", migration.getDescription());
		properties.put("type", getMigrationType(migration).name());
		properties.put("source", migration.getSource());
		migration.getChecksum().ifPresent(checksum -> properties.put("checksum", checksum));

		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Returns the type of the migration in question. It's not part of the API so that it is not possible to be
	 * overwritten by classes implementing {@link JavaBasedMigration}.
	 *
	 * @param migration The migration whose type should be computed
	 * @return The type of the migration.
	 */
	static MigrationType getMigrationType(Migration migration) {

		MigrationType type;
		if (migration instanceof JavaBasedMigration) {
			type = MigrationType.JAVA;
		} else if (migration instanceof CypherBasedMigration) {
			type = MigrationType.CYPHER;
		} else {
			throw new MigrationsException("Unknown migration type: " + migration.getClass());
		}
		return type;
	}

	static String toString(Migration migration) {

		return String.format("%s (\"%s\")", migration.getVersion(), migration.getDescription());
	}

	static class DefaultMigrationContext implements MigrationContext {

		private static final Method WITH_IMPERSONATED_USER = findWithImpersonatedUser();
		private final UnaryOperator<SessionConfig.Builder> applySchemaDatabase;

		private static Method findWithImpersonatedUser() {
			try {
				return SessionConfig.Builder.class.getMethod("withImpersonatedUser", String.class);
			} catch (NoSuchMethodException e) {
				return null; // This is fine
			}
		}

		private final MigrationsConfig config;

		private final Driver driver;

		DefaultMigrationContext(MigrationsConfig config, Driver driver) {

			if (config.getOptionalImpersonatedUser().isPresent() && WITH_IMPERSONATED_USER == null) {
				throw new IllegalArgumentException(
					"User impersonation requires a driver that supports `withImpersonatedUser`.");
			}

			this.config = config;
			this.driver = driver;
			this.applySchemaDatabase = this.config.getOptionalSchemaDatabase().map(schemaDatabase ->
				(UnaryOperator<SessionConfig.Builder>) builder -> builder.withDatabase(schemaDatabase)
			).orElseGet(UnaryOperator::identity);
		}

		@Override
		public MigrationsConfig getConfig() {
			return config;
		}

		@Override
		public Driver getDriver() {
			return driver;
		}

		@Override
		public SessionConfig getSessionConfig() {
			return getSessionConfig(UnaryOperator.identity());
		}

		@Override
		public SessionConfig getSessionConfig(UnaryOperator<SessionConfig.Builder> configCustomizer) {

			SessionConfig.Builder builder = SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE);
			this.config.getOptionalDatabase().ifPresent(builder::withDatabase);
			this.config.getOptionalImpersonatedUser().ifPresent(user -> {
				try {
					// This is fine, when an impersonated user is present, the availability of
					// this method has been checked.
					// noinspection ConstantConditions
					WITH_IMPERSONATED_USER.invoke(builder, user);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new MigrationsException("Could not impersonate a user on the driver level", e);
				}
			});

			return configCustomizer.apply(builder).build();
		}

		@Override
		public Session getSchemaSession() {
			return getDriver().session(getSessionConfig(applySchemaDatabase));
		}
	}
}
