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
package ac.simons.neo4j.migrations.quarkus;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * Must be public for the quarkus test class loader.
 *
 * @author Michael J. Simons
 */
public class Neo4jTestResource implements QuarkusTestResourceLifecycleManager {

	private final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("migrations.default-neo4j-image"));

	@Override
	public Map<String, String> start() {
		this.neo4j.start();
		return Map.of("quarkus.neo4j.uri", this.neo4j.getBoltUrl(), "quarkus.neo4j.authentication.username", "neo4j",
				"quarkus.neo4j.authentication.password", this.neo4j.getAdminPassword());
	}

	@Override
	public void stop() {
		this.neo4j.stop();
	}

}
