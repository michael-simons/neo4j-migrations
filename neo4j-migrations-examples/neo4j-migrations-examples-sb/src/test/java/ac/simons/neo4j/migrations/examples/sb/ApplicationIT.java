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
package ac.simons.neo4j.migrations.examples.sb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIT {

	@Container
	private static final Neo4jContainer neo4j = new Neo4jContainer(
			System.getProperty("migrations.default-neo4j-image"));

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
	}

	Logger logger = LoggerFactory.getLogger(ApplicationIT.class);

	/**
	 * I didn't find any better way to make sure failsafe actually is able to run Spring
	 * Boot's fat jar.
	 * @throws Exception Everything that goes wrong.
	 */
	@Test
	void migrationsShouldHaveRun() throws Exception {

		ProcessBuilder pb = new ProcessBuilder("java", "-jar", System.getProperty("artifact"),
				"--spring.neo4j.uri=" + neo4j.getBoltUrl(), "--spring.neo4j.authentication.username=neo4j",
				"--spring.neo4j.authentication.password=" + neo4j.getAdminPassword());

		pb.redirectErrorStream(true);
		CountDownLatch latch = new CountDownLatch(1);
		Process p = pb.start();
		// Just asserting inside the future or calling a method does *not* work,
		// Even a Assertions.fail("wtf?") is swallowed by "something", and I cannot be
		// bothered atm to investigate that.
		p.onExit().whenComplete((proc, e) -> latch.countDown());

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				this.logger.info(line);
			}
		}

		p.destroy();
		latch.await();

		assertThat(p.exitValue()).isZero();
		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), Config.builder().build());
				Session session = driver.session()) {
			long cnt = session.executeRead(tx -> tx.run("MATCH (n:SomeNode) RETURN count(n)").single().get(0).asLong());
			assertThat(cnt).isEqualTo(2L);

			Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);
			MigrationChain info = migrations.info(MigrationChain.ChainBuilderMode.REMOTE);
			assertThat(info.length()).isEqualTo(5);
			assertThat(info.getLastAppliedVersion().map(MigrationVersion::getValue)).hasValue("040");
			assertThat(info.isApplied("040")).isTrue();

			cnt = session.executeRead(tx -> tx.run("MATCH (n:Person) RETURN count(n)").single().get(0).asLong());
			assertThat(cnt).isEqualTo(1L);
		}
	}

}
