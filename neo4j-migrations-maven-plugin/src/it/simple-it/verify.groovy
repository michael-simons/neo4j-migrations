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
@Grab('org.neo4j.driver:neo4j-java-driver:5.28.10')

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session

Driver driver = GraphDatabase.driver(address, AuthTokens.basic("neo4j", "one-does-not-simply-use-a-6-digit-password-for-an-enterprise-app"))
driver.verifyConnectivity()
Session session = driver.session()

try {
    String version = session.run("MATCH (n:`__Neo4jMigration`) WHERE NOT (n) -[:MIGRATED_TO] -> () RETURN n.version")
            .single().get("n.version")
            .asString()
    assert version == "0002"
} finally {
    session.close()
    driver.close()
}