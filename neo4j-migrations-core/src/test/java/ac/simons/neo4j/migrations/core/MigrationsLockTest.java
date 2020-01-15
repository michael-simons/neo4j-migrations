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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.*;

import ac.simons.neo4j.migrations.core.Migrations.DefaultMigrationContext;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
class MigrationsLockTest extends TestBase {

	@Test
	void shouldAcquireLock() {

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		MigrationsLock lock = new MigrationsLock(context);
		String lockId = lock.lock();

		try (Session session = context.getSession()) {

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

		DefaultMigrationContext context = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
		MigrationsLock lock1 = new MigrationsLock(context);
		lock1.lock();

		try {
			MigrationsLock lock2 = new MigrationsLock(context);
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

		MigrationsConfig migrationsConfig = MigrationsConfig.defaultConfig();
		DefaultMigrationContext context = new DefaultMigrationContext(migrationsConfig, driver);

		try (Session session = context.getSession()) {

			int cnt = session.writeTransaction(
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

				int cnt = session.writeTransaction(
					t -> t.run("MATCH (lock:__Neo4jMigrationsLock) DELETE lock")
						.consume().counters().nodesDeleted());
				assertThat(cnt).isGreaterThanOrEqualTo(2);
			}
			// Remove the shutdown hook to avoid error messages in test because junit already closed the driver
			lock.unlock();
		}
	}
}
