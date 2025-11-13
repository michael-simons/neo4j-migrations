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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRefactoringsITTestBase {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@SuppressWarnings("resource")
	private final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("migrations.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	protected Driver driver;

	protected Neo4jVersion version;

	@BeforeAll
	void initDriver() {

		this.neo4j.start();

		Config config = Config.builder().build();
		this.driver = GraphDatabase.driver(this.neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", this.neo4j.getAdminPassword()), config);
		try (Session session = this.driver.session()) {
			this.version = Neo4jVersion.of(session
				.run("CALL dbms.components() YIELD name, versions WHERE name = 'Neo4j Kernel' RETURN versions[0]")
				.single()
				.get(0)
				.asString());
		}
	}

	boolean connectionSupportsSubqueries() {
		return this.version.compareTo(Neo4jVersion.V3_5) >= 0;
	}

	boolean connectionSupportsCallInTx() {
		return this.version.compareTo(Neo4jVersion.V4_4) >= 0;
	}

	boolean supportsIdentifyingElements() {
		return this.version.compareTo(Neo4jVersion.V4_1) >= 0;
	}

	boolean customQueriesSupported() {
		return connectionSupportsSubqueries() && supportsIdentifyingElements();
	}

}
