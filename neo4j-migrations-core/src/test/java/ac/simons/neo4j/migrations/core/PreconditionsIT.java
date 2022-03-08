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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * @author Michael J. Simons
 */
//@Testcontainers(disabledWithoutDocker = true)
class PreconditionsIT {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@Test
	void assumptionsShouldWork() {

		try (
			Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.3-enterprise")
				.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
				.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		) {
			neo4j.start();
			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				Migrations migrations;
				migrations = new Migrations(MigrationsConfig.builder()
					.withLocationsToScan("classpath:preconditions")
					.build(), driver);
				migrations.apply();

				try (Session session = driver.session()) {

					long cnt = session.run("MATCH (n:__Neo4jMigration) RETURN count(n) AS cnt").single().get("cnt")
						.asLong();
					assertThat(cnt).isEqualTo(2L);
				}
			}
		}
	}

	@Test
	void assertionsShouldWork() {

		try (
			Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.4")
				.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
		) {
			neo4j.start();
			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {
				Migrations migrations;
				migrations = new Migrations(MigrationsConfig.builder()
					.withLocationsToScan("classpath:preconditions")
					.build(), driver);

				assertThatExceptionOfType(MigrationsException.class).isThrownBy(migrations::apply)
					.withMessage("Could not satisfy `// assert that edition is ENTERPRISE`.");
			}
		}
	}

	@Test
	void thingsShouldNotFailWhenAssumptionsChangeDueToVersionUpgrade() {
		try (
			Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.0")
				.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
		) {
			neo4j.start();
			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				Migrations migrations;
				migrations = new Migrations(MigrationsConfig.builder()
					.withLocationsToScan("classpath:preconditions2")
					.build(), driver);

				migrations.apply();
				assertStateBeforeAndAfterPreconditionChanged(driver);

				try (Session session = driver.session()) {
					session.writeTransaction(tx -> tx.run("CREATE(v:Version {name:'4.1'})").consume());
				}

				migrations = new Migrations(MigrationsConfig.builder()
					.withLocationsToScan("classpath:preconditions2")
					.build(), driver);

				migrations.apply();
				assertStateBeforeAndAfterPreconditionChanged(driver);
			}
		}
	}

	private void assertStateBeforeAndAfterPreconditionChanged(Driver driver) {
		try (Session session = driver.session()) {
			long cnt = session.run("MATCH (n:__Neo4jMigration) RETURN count(n) AS cnt").single().get("cnt")
				.asLong();
			assertThat(cnt).isEqualTo(2L);
			cnt = session.run(("MATCH (m:Node {tag: 'I was here (old)'}) RETURN count(m) AS cnt")).single()
				.get("cnt")
				.asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}
}
