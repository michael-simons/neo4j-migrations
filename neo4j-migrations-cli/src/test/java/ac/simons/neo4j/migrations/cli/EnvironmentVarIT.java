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
package ac.simons.neo4j.migrations.cli;

import java.lang.reflect.Field;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.security.InternalAuthToken;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks if known Neo4j environment variables work.
 *
 * @author Michael J. Simons
 */
class EnvironmentVarIT {

	static void assertAuthToken(AuthToken authToken) {
		assertThat(authToken).isInstanceOf(InternalAuthToken.class);
		assertThat(((InternalAuthToken) authToken).toMap()).containsEntry("principal", Values.value("foo"))
			.containsEntry("credentials", Values.value("bar"));
	}

	@Test
	void shouldUseNeo4jAuraDefaultEnv() throws NoSuchFieldException, IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs();

		AuthToken authToken = cli.getAuthToken();
		assertAuthToken(authToken);

		Field addressField = MigrationsCli.class.getDeclaredField("address");
		addressField.setAccessible(true);
		URI addressValue = (URI) addressField.get(cli);
		assertThat(addressValue).hasToString("bolt://aura-nicht-ok:1234");
	}

}
