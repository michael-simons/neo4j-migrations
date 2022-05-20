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

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseCatalogIT {

	@ParameterizedTest
	@EnumSource(value = Neo4jVersion.class, names = { "LATEST", "UNDEFINED" }, mode = EnumSource.Mode.EXCLUDE)
	void shouldBuildCatalog(Neo4jVersion version) throws IOException {

		DefaultCatalog expectedCatalog = new DefaultCatalog();
		expectedCatalog.addAll(MigrationVersion.withValue("1"), () -> Arrays.asList(
			Constraint.forNode("Person").named("c1").key("firstname", "surname"),
			Constraint.forNode("Book").named("c2").exists("isbn"),
			Constraint.forRelationship("LIKED").named("c3").exists("day"),
			Constraint.forNode("Book").named("c4").unique("isbn"),
			Constraint.forRelationship("LIKED").named("c5").exists("x,liked.y"),
			Constraint.forNode("Person").named("c6")
				.key("firstname", "surname", "person.whatever", "person.a,person.b 😱")
		));

		try (Neo4jContainer<?> neo4j = getNeo4j(version)) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config);
				Session session = driver.session()) {
				Catalog catalog;
				if (version == Neo4jVersion.V4_2) {
					catalog = session.writeTransaction(tx -> DatabaseCatalog.of(version, tx));
				} else {
					catalog = session.readTransaction(tx -> DatabaseCatalog.of(version, tx));
				}
				assertThat(catalog.getItems()).isNotEmpty();
				assertThat(catalog.getItems()).allMatch(expectedCatalog::containsEquivalentItem);
			}
		}
	}

	@Test
	void shouldNotSelectBackendIndexesForConstraints() {
		Assertions.fail("see method name");
	}

	private Neo4jContainer<?> getNeo4j(Neo4jVersion version) throws IOException {
		Neo4jContainer<?> neo4j = new Neo4jContainer<>(String.format("neo4j:%s-enterprise", version.toString()))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.withLabel("ac.simons.neo4j.migrations.core", this.getClass().getSimpleName() + "-" + version.name());
		neo4j.start();

		String cypherResource = String.format("/constraints/Create_%s_test_constraints.cypher", version.name());
		try (
			BufferedReader cypherReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
				getClass().getResourceAsStream(cypherResource))));
			Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", neo4j.getAdminPassword()), ConstraintsIT.NO_DRIVER_LOGGING_CONFIG);
			Session session = driver.session()) {
			String cmd;
			while ((cmd = cypherReader.readLine()) != null) {
				if (cmd.trim().isEmpty()) {
					continue;
				}
				assertThat(session.run(cmd).consume().counters().constraintsAdded()).isNotZero();
			}
		}

		return neo4j;
	}
}
