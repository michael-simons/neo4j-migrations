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

import ac.simons.neo4j.migrations.core.MigrationChain.ChainBuilderMode;
import ac.simons.neo4j.migrations.core.ValidationResult.Outcome;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;

/**
 * Main entry to Neo4j Migrations
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class Migrations {

	static final Logger LOGGER = Logger.getLogger(Migrations.class.getName());
	static final Logger STARTUP_LOGGER = Logger.getLogger(Migrations.class.getName() + ".Startup");

	private static final String PROPERTY_MIGRATION_VERSION = "version";
	private static final String PROPERTY_MIGRATION_TARGET = "migrationTarget";
	private static final String PROPERTY_MIGRATION_DESCRIPTION = "description";

	static final Constraint UNIQUE_VERSION =
		Constraint.forNode("__Neo4jMigration")
			.named("unique_version___Neo4jMigration")
			.unique(PROPERTY_MIGRATION_VERSION, PROPERTY_MIGRATION_TARGET);

	private final MigrationsConfig config;
	private final Driver driver;
	private final MigrationContext context;
	private final DiscoveryService discoveryService;
	private final ChainBuilder chainBuilder;

	@SuppressWarnings("squid:S3077")
	private volatile List<Migration> resolvedMigrations;
	@SuppressWarnings("squid:S3077")
	private volatile Map<LifecyclePhase, List<Callback>> resolvedCallbacks;

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

		this.discoveryService = new DiscoveryService(this.config.getMigrationClassesDiscoverer(), this.config.getResourceScanner());
		this.chainBuilder = new ChainBuilder();

		this.context = new DefaultMigrationContext(this.config, this.driver);
	}

	private List<Migration> getMigrations() {

		List<Migration> availableMigrations = this.resolvedMigrations;
		if (availableMigrations == null) {
			synchronized (this) {
				availableMigrations = this.resolvedMigrations;
				if (availableMigrations == null) {
					this.resolvedMigrations = discoveryService.findMigrations(this.context);
					availableMigrations = this.resolvedMigrations;
				}
			}
		}
		return availableMigrations;
	}

	private Map<LifecyclePhase, List<Callback>> getCallbacks() {

		Map<LifecyclePhase, List<Callback>> availableCallbacks = this.resolvedCallbacks;
		if (availableCallbacks == null) {
			synchronized (this) {
				availableCallbacks = this.resolvedCallbacks;
				if (availableCallbacks == null) {
					this.resolvedCallbacks = discoveryService.findCallbacks(this.context);
					availableCallbacks = this.resolvedCallbacks;
				}
			}
		}
		return availableCallbacks;
	}

	/**
	 * @return Information about the connection when migrations are applied, validated and so on.
	 * @see MigrationContext#getConnectionDetails()
	 * @since 1.4.0
	 */
	public ConnectionDetails getConnectionDetails() {
		return context.getConnectionDetails();
	}

	/**
	 * Returns information about the context, the database, all applied and all pending applications.
	 *
	 * @return The chain of migrations.
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @see #info(ChainBuilderMode)
	 * @since 0.0.4
	 */
	public MigrationChain info() {

		return executeWithinLock(() -> chainBuilder.buildChain(context, this.getMigrations()),
			LifecyclePhase.BEFORE_INFO, LifecyclePhase.AFTER_INFO);
	}

	/**
	 * Returns information about the context, the database, all applied and all pending applications.
	 *
	 * @param infoCmd Specify how the chain should be computed
	 * @return The chain of migrations.
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @since 1.4.0
	 */
	public MigrationChain info(ChainBuilderMode infoCmd) {

		return executeWithinLock(() -> chainBuilder.buildChain(context, this.getMigrations(), false,
				infoCmd),
			LifecyclePhase.BEFORE_INFO, LifecyclePhase.AFTER_INFO);
	}

	/**
	 * Applies all discovered Neo4j migrations. Migrations can either be classes implementing {@link JavaBasedMigration}
	 * or Cypher script migrations that are on the classpath or filesystem.
	 *
	 * @return The last applied migration (if any)
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @see #apply(boolean)
	 * @since 0.0.1
	 */
	public Optional<MigrationVersion> apply() {

		return apply(false);
	}

	/**
	 * Applies all discovered Neo4j migrations. Migrations can either be classes implementing {@link JavaBasedMigration}
	 * or Cypher script migrations that are on the classpath or filesystem.
	 * <p>
	 * The startup will be logged to {@code ac.simons.neo4j.migrations.core.Migrations.Startup} and can be individiually
	 * disabled through that logger.
	 *
	 * @param log set to {@literal true} to log connection details prior to applying the migrations
	 * @return The last applied migration (if any)
	 * @throws ServiceUnavailableException in case the driver is not connected
	 * @throws MigrationsException         for everything caused by failing migrations
	 * @since 1.12.0
	 */
	public Optional<MigrationVersion> apply(boolean log) {

		return executeWithinLock(() -> {
			if (log && STARTUP_LOGGER.isLoggable(Level.INFO)) {
				STARTUP_LOGGER.info(() -> Messages.INSTANCE.format("startup_log", getUserAgent(), ConnectionDetailsFormatter.INSTANCE.format(this.getConnectionDetails())));
			}
			apply0(this.getMigrations());
			return getLastAppliedVersion();
		}, LifecyclePhase.BEFORE_MIGRATE, LifecyclePhase.AFTER_MIGRATE);
	}

	/**
	 * Applies one or more refactorings to the target (not the schema) database.
	 *
	 * @param refactorings the refactorings to apply
	 * @return summarized counters
	 * @since 1.13.0
	 */
	public Counters apply(Refactoring... refactorings) {

		if (refactorings == null || refactorings.length == 0) {
			return Counters.empty();
		}

		Neo4jVersion neo4jVersion = Neo4jVersion.of(context.getConnectionDetails().getServerVersion());
		Neo4jEdition neo4jEdition = Neo4jEdition.of(context.getConnectionDetails().getServerEdition());

		CatalogBasedMigration.OperationContext operationContext = new CatalogBasedMigration.OperationContext(
			neo4jVersion, neo4jEdition,
			(VersionedCatalog) context.getCatalog(), context::getSession);

		return Arrays.stream(refactorings)
			.filter(Objects::nonNull)
			.sequential()
			.map(CatalogBasedMigration.Operation::refactorWith)
			.map(op -> op.execute(operationContext))
			.reduce(Counters.empty(), Counters::add);
	}

	/**
	 * Applies one or more migrations to the target (not the schema) database without recording any metadata and also
	 * without acquiring the lock.
	 *
	 * @param resources One or more resources pointing to parsable migration data
	 * @return the number of migrations applied
	 * @since 1.13.0
	 */
	public int apply(URL... resources) {

		int cnt = 0;
		if (resources == null || resources.length == 0) {
			return cnt;
		}

		Map<String, ResourceBasedMigrationProvider> providers = ResourceBasedMigrationProvider.unique().stream()
			.collect(Collectors.toMap(ResourceBasedMigrationProvider::getExtension, Function.identity()));

		List<Migration> migrations = new ArrayList<>();
		for (URL resource : resources) {
			if (resource == null) {
				continue;
			}

			String path = resource.getPath();
			Matcher matcher = MigrationVersion.VERSION_PATTERN.matcher(path);
			if (!matcher.find()) {
				throw new IllegalArgumentException(Messages.INSTANCE.format("errors.invalid_resource_name", path));
			}
			String ext = matcher.group("ext");
			if (!providers.containsKey(ext)) {
				throw new IllegalArgumentException(Messages.INSTANCE.format("errors.unsupported_extension", ext));
			}
			ResourceBasedMigrationProvider provider = providers.get(ext);
			migrations.addAll(provider.handle(ResourceContext.of(resource, config)));
		}

		for (Migration migration : migrations) {
			migration.apply(context);
			LOGGER.info(() -> "Applied " + toString(migration));
			++cnt;
		}

		return cnt;
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
		long constraintsRemoved = deletedChainsWithCounters.counter.constraintsRemoved() + deletedChainsWithCounters.additionalConstraintsRemoved;
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
		final long additionalConstraintsRemoved;

		DeletedChainsWithCounters(List<String> chainsDeleted, SummaryCounters counter) {
			this.chainsDeleted = chainsDeleted;
			this.counter = counter;
			this.additionalConstraintsRemoved = 0L;
		}

		DeletedChainsWithCounters(DeletedChainsWithCounters source, long additionalConstraintsRemoved) {
			this.chainsDeleted = source.chainsDeleted;
			this.counter = source.counter;
			this.additionalConstraintsRemoved = additionalConstraintsRemoved;
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
			DeletedChainsWithCounters deletedChainsWithCounters = session.writeTransaction(tx -> {
				Result result = tx.run(query, Values.parameters(PROPERTY_MIGRATION_TARGET, migrationTarget.orElse(null), "all", all));
				return new DeletedChainsWithCounters(
					result.list(r -> r.get(PROPERTY_MIGRATION_TARGET).asString()),
					result.consume().counters()
				);
			});
			ConnectionDetails cd = context.getConnectionDetails();
			if (all && HBD.is44OrHigher(cd)) {

				Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
				RenderConfig dropConfig = RenderConfig.drop()
					.ifExists()
					.forVersionAndEdition(cd.getServerVersion(), cd.getServerEdition());

				return new DeletedChainsWithCounters(deletedChainsWithCounters,
					session.run(renderer.render(UNIQUE_VERSION, dropConfig)).consume().counters().constraintsRemoved());
			}

			return deletedChainsWithCounters;
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
			List<Migration> migrations = this.getMigrations();
			Optional<String> targetDatabase = config.getOptionalSchemaDatabase();
			try {
				MigrationChain migrationChain = new ChainBuilder(true).buildChain(context, migrations, true, ChainBuilderMode.COMPARE);
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
	 * Retrieves the local catalog, containing constraints and indexes.
	 *
	 * @return the local catalog
	 * @since 1.7.0
	 */
	public Catalog getLocalCatalog() {

		// Retrieving the migrations will initialize the local catalog
		if (getMigrations().isEmpty()) {
			return Catalog.empty();
		}
		return this.context.getCatalog();
	}

	/**
	 * Retrieves the database catalog
	 *
	 * @return the database catalog
	 * @since 1.7.0
	 */
	public Catalog getDatabaseCatalog() {

		return executeWithinLock(() -> {
			try (Session session = context.getSession()) {
				Neo4jVersion neo4jVersion = Neo4jVersion.of(context.getConnectionDetails().getServerVersion());
				return DatabaseCatalog.of(neo4jVersion, session, true);
			}
		}, null, null);
	}

	/**
	 * @return the user agent for Neo4j-Migrations (given in the form {@literal name/version}).
	 * @since 1.2.1
	 */
	public static String getUserAgent() {
		return "neo4j-migrations/" + ProductVersion.getValue();
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
			try {
				if (lock.isLocked()) {
					lock.unlock();
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not unlock… Please check for residues (Nodes labeled `__Neo4jMigrationsLock`).");
			}
		}
	}

	/**
	 * @param phase can be {@literal null}, no callback will be involved then
	 */
	private void invokeCallbacks(LifecyclePhase phase) {

		if (phase == null) {
			return;
		}

		LifecycleEvent event = new DefaultLifecycleEvent(phase, this.context);
		this.getCallbacks().getOrDefault(phase, Collections.emptyList())
			.forEach(callback -> {
				try {
					callback.on(event);
				} catch (Exception e) {
					throw new MigrationsException("Could not invoke " + toString(callback, phase) + ".", e);
				}
				LOGGER.log(Level.INFO, logMessageSupplier(callback, phase));
			});
	}

	static Supplier<String> logMessageSupplier(Callback callback, LifecyclePhase phase) {

		return () -> String.format("Invoked %s.", toString(callback, phase));
	}

	static String toString(Callback callback, LifecyclePhase phase) {
		Optional<String> optionalDescription = callback.getOptionalDescription();
		return optionalDescription
			.map(d -> String.format("\"%s\" %s", d, phase.readable()))
			.orElseGet(() -> String.format("%s callback", phase.toCamelCase()));
	}

	private Optional<MigrationVersion> getLastAppliedVersion() {

		try (Session session = context.getSchemaSession()) {
			Node lastMigration = session.readTransaction(tx ->
				tx.run(
					"MATCH (l:__Neo4jMigration) WHERE coalesce(l.migrationTarget,'<default>') = coalesce($migrationTarget,'<default>') AND NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) RETURN l",
						Collections.singletonMap(PROPERTY_MIGRATION_TARGET, config.getMigrationTargetIn(context).orElse(null)))
				.single().get(0).asNode());

			String version = lastMigration.get(PROPERTY_MIGRATION_VERSION).asString();
			String description = lastMigration.get(PROPERTY_MIGRATION_DESCRIPTION).asString();

			return Optional.of(MigrationVersion.withValueAndDescription(version, description, lastMigration.get("repeatable").asBoolean(false)));
		} catch (NoSuchRecordException e) {
			return Optional.empty();
		}
	}

	static void ensureConstraints(MigrationContext context) {

		// Composite unique constraints are not supported here
		if (!HBD.is44OrHigher(context.getConnectionDetails())) {
			return;
		}

		ConnectionDetails cd = context.getConnectionDetails();
		try (Session session = context.getSchemaSession()) {
			Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
			RenderConfig createConfig = RenderConfig.create().forVersionAndEdition(cd.getServerVersion(), cd.getServerEdition());

			final String stmt = renderer.render(UNIQUE_VERSION, createConfig);
			HBD.silentCreateConstraint(context.getConnectionDetails(), session, stmt, null, () -> "Could not create unique constraint for targeted migrations.");
		}
	}

	@SuppressWarnings("deprecation")
	boolean checksumOfRepeatableChanged(MigrationChain currentChain, Migration migration) {

		if (!migration.getVersion().isRepeatable()) {
			return false;
		}
		if (migration instanceof JavaBasedMigration) {
			return true;
		}
		Optional<String> appliedChecksum = currentChain.getElements().stream()
			.filter(e -> e.getVersion().equals(migration.getVersion().getValue()))
			.findFirst()
			.flatMap(MigrationChain.Element::getChecksum);
		return !ChainBuilder.matches(appliedChecksum, migration);
	}

	private void apply0(List<Migration> migrations) {

		ensureConstraints(context);

		MigrationVersion previousVersion = getLastAppliedVersion().orElseGet(MigrationVersion::baseline);

		// Validate and build the chain of migrations
		MigrationChain chain = chainBuilder.buildChain(context, migrations);

		StopWatch stopWatch = new StopWatch();
		for (Migration migration : migrations) {

			boolean repeated = false;
			Supplier<String> logMessage = () -> String.format("Applied migration %s.", toString(migration));
			if (previousVersion != MigrationVersion.baseline() && chain.isApplied(migration.getVersion().getValue())) {
				if (checksumOfRepeatableChanged(chain, migration)) {
					logMessage = () -> String.format("Reapplied changed repeatable migration %s", toString(migration));
					repeated = true;
				} else {
					LOGGER.log(Level.INFO, "Skipping already applied migration {0}", toString(migration));
					continue;
				}
			}

			try {
				stopWatch.start();
				migration.apply(context);
				long executionTime = stopWatch.stop();
				previousVersion = recordApplication(chain.getUsername(), previousVersion, migration, executionTime, repeated);

				LOGGER.log(Level.INFO, logMessage);
			} catch (Exception e) {
				if (HBD.constraintProbablyRequiredEnterpriseEdition(e, getConnectionDetails())) {
					throw new MigrationsException(Messages.INSTANCE.format("errors.edition_mismatch", toString(migration), getConnectionDetails().getServerEdition()));
				} else if (e instanceof MigrationsException) {
					throw e;
				}
				throw new MigrationsException("Could not apply migration: " + toString(migration) + ".", e);
			} finally {
				stopWatch.reset();
			}
		}
	}

	private MigrationVersion recordApplication(String neo4jUser, MigrationVersion previousVersion, Migration appliedMigration, long executionTime, boolean repeated) {

		Optional<String> migrationTarget = context.getConfig().getMigrationTargetIn(context);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("neo4jUser", neo4jUser);
		parameters.put("previousVersion", previousVersion.getValue());
		parameters.put("appliedMigration", toProperties(appliedMigration));
		parameters.put("installedBy", config.getOptionalInstalledBy().map(Values::value).orElse(Values.NULL));
		parameters.put("executionTime", executionTime);
		parameters.put(PROPERTY_MIGRATION_TARGET, migrationTarget.orElse(null));

		TransactionWork<ResultSummary> uow;
		if (repeated) {
			uow = t -> t.run(
				"MATCH (l:__Neo4jMigration) WHERE l.version = $appliedMigration['version'] AND coalesce(l.migrationTarget,'<default>') = coalesce($migrationTarget,'<default>') WITH l "
					+ "CREATE (l) - [:REPEATED {checksum: $appliedMigration['checksum'], at: datetime({timezone: 'UTC'}), in: duration( {milliseconds: $executionTime} ), by: $installedBy, connectedAs: $neo4jUser}] -> (l)",
				parameters).consume();
		} else {
			uow = t -> {
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
			};
		}

		try (Session session = context.getSchemaSession()) {
			session.writeTransaction(uow);
		}

		return appliedMigration.getVersion();
	}

	@SuppressWarnings("deprecation")
	private static Map<String, Object> toProperties(Migration migration) {

		Map<String, Object> properties = new HashMap<>();

		properties.put(PROPERTY_MIGRATION_VERSION, migration.getVersion().getValue());
		migration.getOptionalDescription().ifPresent(v -> properties.put(PROPERTY_MIGRATION_DESCRIPTION, v));
		properties.put("type", getMigrationType(migration).name());
		properties.put("repeatable", migration.getVersion().isRepeatable());
		properties.put("source", migration.getSource());
		migration.getChecksum().ifPresent(v -> properties.put("checksum", v));

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
		} else if (migration instanceof AbstractCypherBasedMigration) {
			type = MigrationType.CYPHER;
		} else if (migration instanceof CatalogBasedMigration) {
			type = MigrationType.CATALOG;
		} else {
			throw new MigrationsException("Unknown migration type: " + migration.getClass());
		}
		return type;
	}

	static String toString(Migration migration) {

		return migration.getVersion().getValue()
			+ migration.getOptionalDescription().map(d -> String.format(" (\"%s\")", d)).orElse("");
	}
}
