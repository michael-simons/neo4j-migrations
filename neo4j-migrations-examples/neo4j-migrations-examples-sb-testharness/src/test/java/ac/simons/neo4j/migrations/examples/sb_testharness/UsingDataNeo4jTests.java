/*
 * Copyright 2020-2026 the original author or authors.
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
package ac.simons.neo4j.migrations.examples.sb_testharness;

import ac.simons.neo4j.migrations.springframework.boot.autoconfigure.MigrationsAutoConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.neo4j.test.autoconfigure.DataNeo4jTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example uses Test-Harness 4.3 and requires JDK 11. Test-Harness 3.5 is compatible with
 * JDK 8 and looks like this: <blockquote><pre>
 * {@code
 *    private static ServerControls embeddedDatabaseServer;
 *
 *    &#64;BeforeAll
 *    static void initializeNeo4j() {
 *        embeddedDatabaseServer = TestServerBuilders.newInProcessBuilder().newServer();
 *    }
 *  }
 *  </pre></blockquote>
 *
 * @author Michael J. Simons
 */
@DataNeo4jTest
@ImportAutoConfiguration(MigrationsAutoConfiguration.class)
class UsingDataNeo4jTests {

	private static Neo4j embeddedDatabaseServer;

	@BeforeAll
	static void initializeNeo4j() {

		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {

		registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI);
		registry.add("spring.neo4j.authentication.password", () -> "");
	}

	@AfterAll
	static void closeNeo4j() {
		embeddedDatabaseServer.close();
	}

	@Test
	void migrationsShouldHaveBeenApplied(@Autowired Driver driver) {

		try (Session session = driver.session()) {
			long cnt = session.executeRead(tx -> tx.run("MATCH (n:SomeNode) RETURN count(n)").single().get(0).asLong());
			assertThat(cnt).isEqualTo(1L);

			String version = session.executeRead(
					tx -> tx.run("MATCH (n:`__Neo4jMigration`) WHERE NOT ((n)-[:MIGRATED_TO]->()) return n.version")
						.single()
						.get(0)
						.asString());
			assertThat(version).isEqualTo("0002");
		}
	}

}
