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
package ac.simons.neo4j.migrations.quarkus;

import ac.simons.neo4j.migrations.core.Migrations;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.WithTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@WithTestResource(Neo4jTestResource.class)
class WithDefaultLocationIT {

	@RegisterExtension
	static QuarkusUnitTest test = new QuarkusUnitTest().withConfigurationResource("application.properties")
		.withApplicationRoot(archive -> {
			archive.addAsResource("neo4j/migrations");
			archive.addAsResource("neo4j/secondary-migrations");
		});

	@Inject
	Driver driver;

	@Inject
	Migrations migrations;

	@Test
	void migrationsShouldHaveBeenApplied() {
		try (Session session = this.driver.session()) {
			var lastNode = session
				.run("MATCH (n:__Neo4jMigration) WHERE NOT (n)-[:MIGRATED_TO]->(:__Neo4jMigration) RETURN n")
				.single()
				.get("n")
				.asNode();
			assertThat(lastNode.get("version").asString()).isEqualTo("0001");
			assertThat(lastNode.get("description").asString()).isEqualTo("Default Location");
		}
	}

	@AfterEach
	void clean() {
		this.migrations.clean(true);
	}

}
