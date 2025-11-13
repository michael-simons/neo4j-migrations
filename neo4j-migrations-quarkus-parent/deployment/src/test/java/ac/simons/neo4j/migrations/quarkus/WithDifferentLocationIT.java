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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import ac.simons.neo4j.migrations.core.MigrationChain;
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
class WithDifferentLocationIT {

	static Path p;
	static {
		try {
			var dir = Files.createTempDirectory("more-migrations");
			p = dir.resolve("V0002__2nd_migration.cypher");
			Files.writeString(p, "CREATE (n:IWasHere) RETURN n");
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@RegisterExtension
	static QuarkusUnitTest test = new QuarkusUnitTest().withConfigurationResource("application.properties")
		.overrideConfigKey("org.neo4j.migrations.locations-to-scan", "classpath:neo4j/secondary-migrations")
		.overrideConfigKey("org.neo4j.migrations.external-locations", p.getParent().toUri().toString())
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
			assertThat(lastNode.get("version").asString()).isEqualTo("0002");
			assertThat(lastNode.get("description").asString()).isEqualTo("2nd migration");
		}

		var elements = this.migrations.info().getElements();
		assertThat(elements).extracting(MigrationChain.Element::getOptionalDescription)
			.filteredOn(Optional::isPresent)
			.hasSize(2)
			.element(0)
			.extracting(Optional::get)
			.isEqualTo("2nd location");
	}

	@AfterEach
	void clean() {
		this.migrations.clean(true);
	}

}
