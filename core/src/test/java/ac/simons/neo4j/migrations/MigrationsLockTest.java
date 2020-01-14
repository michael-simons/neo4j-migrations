/*
 * Copyright 2020 the original author or authors.
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
package ac.simons.neo4j.migrations;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.Neo4jException;

/**
 * @author Michael J. Simons
 */
class MigrationsLockTest extends TestBase {

	@Test
	void shouldAcquireLock() {

		MigrationsLock lock = new MigrationsLock(new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver));
		String lockId = lock.lock();

		try (Session session = driver.session()) {

			long cnt = session.run("MATCH (l:__Neo4jMigrationsLock {id: $id}) RETURN count(l) AS cnt",
				Collections.singletonMap("id", lockId)).single()
				.get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		} finally {

			lock.unlock();
		}
	}

	@Test
	void shouldFailIfThereIsALock() {

		MigrationsLock lock1 = new MigrationsLock(new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver));
		lock1.lock();

		try {
			MigrationsLock lock2 = new MigrationsLock(new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver));
			lock2.lock();
			fail("Should not be able to acquire a 2nd lock");
		} catch (MigrationsException e) {

			assertThat(e.getMessage()).isEqualTo(
				"Cannot create __Neo4jMigrationsLock node. Likely another migration is going on or has crashed");
		} finally {

			lock1.unlock();
		}
	}

	@Test
	void shouldDealWithUniquenessProblems() {

		try (Session session = driver.session(SessionConfig.defaultConfig())) {

			dropConstraint(session, "DROP CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.id IS UNIQUE");
			dropConstraint(session, "DROP CONSTRAINT ON (lock:__Neo4jMigrationsLock) ASSERT lock.name IS UNIQUE");
			int cnt = session.writeTransaction(
				t -> t.run("UNWIND range(1,2) AS i WITH i CREATE (:__Neo4jMigrationsLock {id: i, name: 'Foo'})")
					.consume().counters().nodesCreated());
			assertThat(cnt).isEqualTo(2);
		}

		MigrationsLock lock = new MigrationsLock(new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver));
		try {
			lock.lock();
			fail("Should not be able to acquire a lock");
		} catch (MigrationsException e) {

			assertThat(e.getMessage()).startsWith("Could not ensure uniqueness of __Neo4jMigrationsLock. ");
		} finally {

			try (Session session = driver.session(SessionConfig.defaultConfig())) {

				int cnt = session.writeTransaction(
					t -> t.run("MATCH (lock:__Neo4jMigrationsLock) DELETE lock")
						.consume().counters().nodesDeleted());
				assertThat(cnt).isGreaterThanOrEqualTo(2);
			}
			// Remove the shutdown hook to avoid error messages in test because junit already closed the driver
			lock.unlock();
		}
	}

	void dropConstraint(Session session, String constraint) {
		try {
			session.writeTransaction(t -> t.run(constraint).consume());
		} catch (Neo4jException e) {
		}
	}
}
