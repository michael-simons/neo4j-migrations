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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

/**
 * @author Michael J. Simons
 */
class MigrationsLockIT extends TestBase {

	@Test
	void shouldAcquireLock() {

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		MigrationsLock lock = new MigrationsLock(context);
		String lockId = lock.lock();

		assertThat(lock.isLocked()).isTrue();
		try (Session session = context.getSession()) {

			long cnt = session.run("MATCH (l:__Neo4jMigrationsLock {id: $id}) RETURN count(l) AS cnt",
					Collections.singletonMap("id", lockId)).single()
				.get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		} finally {

			lock.unlock();
		}

		assertThat(lock.isLocked()).isFalse();
	}

	@Test
	void shouldNotFailIfTheConstraintExistsAsUnnamedConstraint() {

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		var query = isModernNeo4j(context.getConnectionDetails()) ?
			"CREATE CONSTRAINT FOR (lock:__Neo4jMigrationsLock) REQUIRE lock.id IS UNIQUE" :
			"CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE";
		try (Session session = driver.session()) {
			int cnt = session.executeWrite(t -> t.run(query).consume().counters().constraintsAdded());
			assertThat(cnt).isOne();
		}

		MigrationsLock lock1 = new MigrationsLock(context);
		try {
			assertThatNoException().isThrownBy(lock1::lock);
		} finally {
			lock1.unlock();
		}
	}

	@Test
	void shouldNotFailIfTheConstraintExistsAsNamedConstraint() {

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		String query;
		String validationQuery;
		if (isModernNeo4j(context.getConnectionDetails())) {
			query = "CREATE CONSTRAINT a_name FOR (lock:__Neo4jMigrationsLock) REQUIRE lock.id IS UNIQUE";
			validationQuery = "CREATE CONSTRAINT FOR (lock:__Neo4jMigrationsLock) REQUIRE lock.id IS UNIQUE";
		} else {
			query = "CREATE CONSTRAINT a_name ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE";
			validationQuery = "CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE";
		}
		try (Session session = driver.session()) {
			int cnt = session.executeWrite(
				t -> t.run(query).consume().counters().constraintsAdded());
			assertThat(cnt).isOne();

			// Assert that the exception we want to catch is actually thrown
			assertThatExceptionOfType(Neo4jException.class).isThrownBy(
					() -> session.run(validationQuery).consume())
				.matches(e -> "Neo.ClientError.Schema.ConstraintAlreadyExists".equals(e.code()));
		}

		MigrationsLock lock1 = new MigrationsLock(context);
		try {
			assertThatNoException().isThrownBy(lock1::lock);
		} finally {
			lock1.unlock();
		}
	}

	@Test
	void shouldFailIfThereIsALock() {

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		MigrationsLock lock1 = new MigrationsLock(context);
		lock1.lock();

		try {
			MigrationsLock lock2 = new MigrationsLock(context);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(lock2::lock).withMessage(
				"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed");
		} finally {
			lock1.unlock();
		}
	}

	@Test
	void shouldDealWithUniquenessProblems() {

		MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
		DefaultMigrationContext context = new DefaultMigrationContext(migrationsConfig, driver);

		try (Session session = context.getSession()) {

			int cnt = session.executeWrite(
				t -> t.run("UNWIND range(1,2) AS i WITH i CREATE (:__Neo4jMigrationsLock {id: i, name: 'Foo'})")
					.consume().counters().nodesCreated());
			assertThat(cnt).isEqualTo(2);
		}

		MigrationsLock lock = new MigrationsLock(context);
		try {
			lock.lock();
			fail("Should not be able to acquire a lock");
		} catch (MigrationsException e) {

			assertThat(e.getMessage()).startsWith("Could not ensure uniqueness of __Neo4jMigrationsLock.");
		} finally {

			try (Session session = context.getSession()) {

				int cnt = session.executeWrite(
					t -> t.run("MATCH (lock:__Neo4jMigrationsLock) DELETE lock")
						.consume().counters().nodesDeleted());
				assertThat(cnt).isGreaterThanOrEqualTo(2);
			}
			// Remove the shutdown hook to avoid error messages in test because junit already closed the driver
			lock.unlock();
		}
	}

	@Test
	void shouldUseNamedLock() {
		MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
		DefaultMigrationContext context = new DefaultMigrationContext(migrationsConfig, driver);

		MigrationsLock lock = new MigrationsLock(context);
		try {
			lock.lock();
			try (Session session = context.getSession()) {
				long cnt = session.run(
						"SHOW CONSTRAINTS YIELD name WHERE name =~ '__Neo4jMigrationsLock__has_unique.*' RETURN count(*)")
					.single().get(0).asLong();
				assertThat(cnt).isEqualTo(2);
			}
		} finally {
			// Remove the shutdown hook to avoid error messages in test because junit already closed the driver
			lock.unlock();
		}
	}

	@Test
	void cleanCleansAlsoUnnamedConstraints() {

		Migrations migrations = new Migrations(MigrationsConfig.builder()
			.withSchemaDatabase("neo4j")
			.build(), driver);

		String idConstraint;
		String nameConstraint;
		if (isModernNeo4j(migrations.getConnectionDetails())) {
			idConstraint = "CREATE CONSTRAINT FOR (lock:__Neo4jMigrationsLock) REQUIRE lock.id IS UNIQUE";
			nameConstraint = "CREATE CONSTRAINT FOR (lock:__Neo4jMigrationsLock) REQUIRE lock.name IS UNIQUE";
		} else {
			idConstraint = "CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE";
			nameConstraint = "CREATE CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.name IS UNIQUE";
		}

		// Create old-school constraints
		try (Session session = driver.session()) {
			int cnt = session.executeWrite(t -> t.run(idConstraint).consume().counters().constraintsAdded());
			assertThat(cnt).isOne();
			cnt = session.executeWrite(t -> t.run(nameConstraint).consume().counters().constraintsAdded());
			assertThat(cnt).isOne();
		}

		CleanResult cleanResult = migrations.clean(true);

		assertThat(cleanResult.getChainsDeleted()).isEmpty();
		assertThat(cleanResult.getConstraintsRemoved()).isEqualTo(2);
		assertThat(cleanResult.getIndexesRemoved()).isZero();
		assertThat(cleanResult.getNodesDeleted()).isZero();
		assertThat(cleanResult.getRelationshipsDeleted()).isZero();
		assertThat(TestBase.allLengthOfMigrations(driver, "neo4j")).isEmpty();

		try (Session session = driver.session()) {
			int numConstraints = session.run("SHOW CONSTRAINTS YIELD name RETURN count(*) AS numConstraints").single()
				.get(0).asInt();
			assertThat(numConstraints).isZero();
		}
	}
}
