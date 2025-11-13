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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SkipArm64IncompatibleConfiguration.class)
class DefaultMigrationContextIT {

	@ParameterizedTest
	@ArgumentsSource(SkipArm64IncompatibleConfiguration.VersionProvider.class)
	void shouldUseShowProceduresIfNecessary(SkipArm64IncompatibleConfiguration.VersionUnderTest version) {

		try (Neo4jContainer neo4j = getNeo4j(version.asTag())) {

			Config config = Config.builder().withLogging(Logging.none()).build();
			try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(),
					AuthTokens.basic("neo4j", neo4j.getAdminPassword()), config)) {

				DefaultMigrationContext ctx = new DefaultMigrationContext(MigrationsConfig.defaultConfig(), driver);
				ConnectionDetails connectionDetails = ctx.getConnectionDetails();
				assertThat(connectionDetails.getUsername()).isEqualTo("neo4j");
			}
		}
	}

	private Neo4jContainer getNeo4j(String tag) {
		Neo4jContainer neo4j = new Neo4jContainer(tag).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes").withReuse(true);
		neo4j.start();
		return neo4j;
	}

}
