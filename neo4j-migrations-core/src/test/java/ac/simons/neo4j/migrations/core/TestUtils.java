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

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * @author Michael J. Simons
 */
final class TestUtils {

	static void clearDatabase(Driver driver, String database) {

		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			session.run("MATCH (n) DETACH DELETE n");
		}
	}

	static int lengthOfMigrations(Driver driver, String database) {

		SessionConfig sessionConfig = getSessionConfig(database);

		try (Session session = driver.session(sessionConfig)) {
			return session.run(""
				+ "MATCH p=(b:__Neo4jMigration {version:'BASELINE'}) - [:MIGRATED_TO*] -> (l:`__Neo4jMigration`) "
				+ "WHERE NOT (l)-[:MIGRATED_TO]->(:__Neo4jMigration) "
				+ "RETURN length(p) AS l").single().get("l").asInt();
		}
	}

	private static SessionConfig getSessionConfig(String database) {
		SessionConfig sessionConfig;
		if (database == null) {
			sessionConfig = SessionConfig.defaultConfig();
		} else {
			sessionConfig = SessionConfig.forDatabase(database);
		}
		return sessionConfig;
	}

	private TestUtils() {
	}
}
