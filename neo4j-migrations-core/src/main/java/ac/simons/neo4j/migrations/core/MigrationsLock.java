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

import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;

/**
 * @author Michael J. Simons
 * @since 0.0.1
 */
final class MigrationsLock {

	private static final Logger LOGGER = Logger.getLogger(MigrationsLock.class.getName());
	private static final String DEFAULT_NAME_OF_LOCK = "John Doe";
	private static final Supplier<String> LOCK_FAILED_MESSAGE_SUPPLIER = () -> Messages.INSTANCE.get("lock_failed");
	private static final Renderer<Constraint> CONSTRAINT_RENDERER = Renderer.get(Renderer.Format.CYPHER, Constraint.class);

	static final Constraint[] REQUIRED_CONSTRAINTS = new Constraint[] {
		Constraint.forNode("__Neo4jMigrationsLock")
			.named("__Neo4jMigrationsLock__has_unique_id")
			.unique("id"),

		Constraint.forNode("__Neo4jMigrationsLock")
			.named("__Neo4jMigrationsLock__has_unique_name")
			.unique("name")
	};

	private final MigrationContext context;
	private final String id = UUID.randomUUID().toString();
	private final String nameOfLock;

	private final ReentrantReadWriteLock lockLockingLocks = new ReentrantReadWriteLock();

	private final Thread cleanUpTask = new Thread(this::unlock0);

	MigrationsLock(MigrationContext context) {
		this.context = context;
		this.nameOfLock = context.getConfig().getOptionalDatabase()
			.map(v -> v.toLowerCase(Locale.ROOT))
			// Prior to 1.1, Migrations would use "John Doe" by default, so if someone used explicitly "neo4j" as database
			// name, the lock wouldn't work. Therefore, we must translate this here.

			// "John Doe" will be unique and not clash with any existing database name, see
			// - Length must be between 3 and 63 characters.
			// - The first character of a name must be an ASCII alphabetic character.
			// - Subsequent characters must be ASCII alphabetic or numeric characters, dots or dashes; [a..z][0..9].
			// From https://neo4j.com/docs/operations-manual/current/manage-databases/configuration/
			.filter(v -> !v.equals("neo4j"))
			.orElse(DEFAULT_NAME_OF_LOCK);
	}

	private void createUniqueConstraintIfNecessary() {

		int constraintsAdded = 0;
		ConnectionDetails cd = context.getConnectionDetails();
		try (Session session = context.getSchemaSession()) {
			Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
			RenderConfig createConfig = RenderConfig.create().forVersionAndEdition(cd.getServerVersion(), cd.getServerEdition());

			for (Constraint constraint : REQUIRED_CONSTRAINTS) {
				String cypher = renderer.render(constraint, createConfig);
				try {
					constraintsAdded += HBD.silentCreateConstraint(cd, session, cypher, null, LOCK_FAILED_MESSAGE_SUPPLIER);
				} catch (MigrationsException e) {
					if (!constraintWithNameAlreadyExistsAndIsEquivalent(cd, session, constraint, e)) {
						throw e;
					}
				}
			}
		}
		LOGGER.log(Level.FINE, "Created {0} constraints", constraintsAdded);
	}

	private static boolean constraintWithNameAlreadyExistsAndIsEquivalent(ConnectionDetails cd, Session session, Constraint constraint, MigrationsException e) {
		return e.getCause() instanceof Neo4jException ne && Neo4jCodes.CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE.equals(ne.code()) &&
			DatabaseCatalog.full(Neo4jVersion.of(cd.getServerVersion()), session).containsEquivalentItem(constraint);
	}

	SummaryCounters clean() {

		int nodesDeleted = 0;
		int relationshipsDeleted = 0;
		int constraintsRemoved = 0;
		int indexesRemoved = 0;

		ConnectionDetails cd = context.getConnectionDetails();
		try (Session session = context.getSchemaSession()) {
			SummaryCounters summaryCounters = session.executeWrite(
				tx -> tx.run("MATCH (l:`__Neo4jMigrationsLock`) delete l").consume().counters());
			nodesDeleted += summaryCounters.nodesDeleted();
			relationshipsDeleted += summaryCounters.relationshipsDeleted();

			RenderConfig dropConfig = RenderConfig.drop().forVersionAndEdition(cd.getServerVersion(), cd.getServerEdition());
			for (Constraint constraint : REQUIRED_CONSTRAINTS) {
				String cypher = CONSTRAINT_RENDERER.render(constraint, dropConfig);
				Integer singleConstraint = HBD.silentDropConstraint(cd, session, cypher, null);
				if (singleConstraint == 0) {
					// Enforce unnamed
					cypher = CONSTRAINT_RENDERER.render(constraint, dropConfig.ignoreName());
					singleConstraint = HBD.silentDropConstraint(cd, session, cypher, null);
				}
				constraintsRemoved += singleConstraint;
			}
		}

		return new SummaryCountersImpl(
			0, nodesDeleted,
			0, relationshipsDeleted,
			0,
			0, 0,
			0, indexesRemoved,
			0, constraintsRemoved, 0
		);
	}

	record SummaryCountersImpl(int nodesCreated, int nodesDeleted, int relationshipsCreated,
		int relationshipsDeleted, int propertiesSet, int labelsAdded, int labelsRemoved,
		int indexesAdded, int indexesRemoved, int constraintsAdded, int constraintsRemoved,
		int systemUpdates
	) implements SummaryCounters {

		@Override
		public boolean containsUpdates() {
			return (nodesCreated | nodesDeleted | relationshipsCreated | relationshipsDeleted | propertiesSet | labelsAdded | labelsRemoved | indexesAdded | indexesRemoved | constraintsAdded | constraintsRemoved) > 0;
		}

		@Override
		public boolean containsSystemUpdates() {
			return systemUpdates > 0;
		}
	}

	String lock() {

		if (LOGGER.isLoggable(Level.FINE)) {
			MigrationsConfig config = context.getConfig();
			UnaryOperator<String> databaseNameMapper = v -> "database `" + v + "`";
			String formattedTargetDatabaseName = config.getOptionalDatabase().map(databaseNameMapper).orElse("the default database");
			LOGGER.log(Level.FINE, "Acquiring lock {0} on {1} in {2}", new Object[] { id, formattedTargetDatabaseName, config
				.getOptionalSchemaDatabase().map(databaseNameMapper).orElse(formattedTargetDatabaseName) });
		}

		createUniqueConstraintIfNecessary();

		try (Session session = context.getSchemaSession()) {
			lockLockingLocks.writeLock().lock();
			var internalId = session.executeWrite(t ->
				t.run("CREATE (l:__Neo4jMigrationsLock {id: $id, name: $name}) RETURN l",
					Values.parameters("id", id, "name", nameOfLock))
					.single().get("l").asNode().elementId()
			);
			LOGGER.log(Level.FINE, "Acquired lock {0} with internal id {1}", new Object[] { id, internalId });
			Runtime.getRuntime().addShutdownHook(cleanUpTask);
			return id;
		} catch (Neo4jException e) {
			throw new MigrationsException(
				"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed", e);
		} finally {
			lockLockingLocks.writeLock().unlock();
		}
	}

	boolean isLocked() {

		try (Session session = context.getSchemaSession()) {
			lockLockingLocks.readLock().lock();
			return session.executeRead(
				tx -> tx.run("MATCH (l:__Neo4jMigrationsLock {id: $id, name: $name}) RETURN count(l)",
						Values.parameters("id", id, "name", nameOfLock))
					.single().get(0).asLong() > 0);
		} finally {
			lockLockingLocks.readLock().unlock();
		}
	}

	void unlock() {

		try {
			unlock0();
		} finally {
			Runtime.getRuntime().removeShutdownHook(cleanUpTask);
		}
	}

	private void unlock0() {

		try (Session session = context.getSchemaSession()) {

			ResultSummary resultSummary = session.executeWrite(t ->
				t.run("MATCH (l:__Neo4jMigrationsLock {id: $id}) DELETE l", Values.parameters("id", id))
					.consume());
			LOGGER.log(Level.FINE, "Released lock {0} ({1} node(s) deleted)",
				new Object[] { id, resultSummary.counters().nodesDeleted() });
		}
	}
}
