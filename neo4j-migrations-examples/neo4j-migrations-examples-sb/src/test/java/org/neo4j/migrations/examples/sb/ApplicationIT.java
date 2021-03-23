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
package org.neo4j.migrations.examples.sb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIT {

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
	}

	Logger logger = LoggerFactory.getLogger(ApplicationIT.class);

	@Container
	private static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.2")
		.withReuse(TestcontainersConfiguration.getInstance().environmentSupportsReuse());

	/**
	 * I didn't find any better way to make sure failsafe actually is able to run Spring Boot's fat jar.
	 *
	 * @throws Exception Everything that goes wrong.
	 */
	@Test
	void migrationsShouldHaveRun() throws Exception {

		ProcessBuilder pb = new ProcessBuilder("java", "-jar",
			System.getProperty("artifact"),
			"--spring.neo4j.uri=" + neo4j.getBoltUrl(),
			"--spring.neo4j.authentication.username=neo4j",
			"--spring.neo4j.authentication.password=" + neo4j.getAdminPassword()
		);

		pb.redirectErrorStream(true);
		Process p = pb.start();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				logger.info(line);
			}
		}

		try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()),
			Config.builder().withLogging(Logging.console(Level.OFF)).build());
			Session session = driver.session()
		) {
			long cnt = session.readTransaction(tx -> tx.run("MATCH (n:SomeNode) RETURN count(n)").single().get(0).asLong());
			assertThat(cnt).isEqualTo(1L);
		}
	}
}
