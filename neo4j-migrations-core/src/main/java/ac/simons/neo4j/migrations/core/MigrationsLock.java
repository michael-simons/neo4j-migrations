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

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.summary.ResultSummary;

/**
 * @author Michael J. Simons
 * @since 0.0.1
 */
final class MigrationsLock {

	private static final Logger LOGGER = Logger.getLogger(MigrationsLock.class.getName());
	private static final String nameOfLock = "John Doe";

	private final MigrationContext context;
	private final String id = UUID.randomUUID().toString();

	private final Thread cleanUpTask = new Thread(this::unlock0);

	MigrationsLock(MigrationContext context) {
		this.context = context;
	}

	void createUniqueConstraintIfNecessary() {

		try (Session session = context.getSession()) {
			int numberOfConstraints = session.writeTransaction(t -> {
				int rv = t.run("CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE").consume()
					.counters().constraintsAdded();
				rv += t.run("CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.name IS UNIQUE").consume()
					.counters().constraintsAdded();
				return rv;
			});
			LOGGER.log(Level.FINE, "Created {0} constraints", numberOfConstraints);
		} catch (Neo4jException e) {

			if (!"Neo.ClientError.Schema.EquivalentSchemaRuleAlreadyExists".equals(e.code())) {
				throw new MigrationsException(""
					+ "Could not ensure uniqueness of __Neo4jMigrationsLock. "
					+ "Please make sure your instance is in a clean state, "
					+ "no more than 1 lock should be there simultaneously!",
					e
				);
			}
		}
	}

	public String lock() {

		LOGGER.log(Level.FINE, "Acquiring lock {0} on database", id);

		createUniqueConstraintIfNecessary();

		try (Session session = context.getSession()) {

			long internalId = session.writeTransaction(t ->
				t.run("CREATE (l:__Neo4jMigrationsLock {id: $id, name: $name}) RETURN l",
					Values.parameters("id", id, "name", nameOfLock))
					.single().get("l").asNode().id()
			);
			LOGGER.log(Level.FINE, "Acquired lock {0} with internal id {1}", new Object[] { id, internalId });
			Runtime.getRuntime().addShutdownHook(cleanUpTask);
			return id;
		} catch (Neo4jException e) {
			throw new MigrationsException(
				"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed", e);
		}
	}

	public void unlock() {

		try {
			unlock0();
		} finally {
			Runtime.getRuntime().removeShutdownHook(cleanUpTask);
		}
	}

	void unlock0() {

		try (Session session = context.getSession()) {

			ResultSummary resultSummary = session.writeTransaction(t ->
				t.run("MATCH (l:__Neo4jMigrationsLock {id: $id}) DELETE l", Values.parameters("id", id))
					.consume());
			LOGGER.log(Level.FINE, "Released lock {0} ({1} node(s) deleted)",
				new Object[] { id, resultSummary.counters().nodesDeleted() });
		}
	}
}
