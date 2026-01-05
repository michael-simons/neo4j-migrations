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
package ac.simons.neo4j.migrations.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings({ "squid:S2187" }) // Sonar doesn't realize that there are tests
class CypherBasedCallbackTests {

	@Nested
	class Descriptions {

		@Test
		void emptyDescriptionShouldBeHandled() throws MalformedURLException {

			CypherBasedCallback callback = new CypherBasedCallback(
					ResourceContext.of(new URL("file:./afterMigrate.cypher")));
			assertThat(callback.getOptionalDescription()).isEmpty();
		}

		@Test
		void shouldNotFailOnInvalidName() throws MalformedURLException {

			URL url = new URL("file:./afterMigrate__.cypher");
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> new CypherBasedCallback(ResourceContext.of(url)))
				.withMessage("Invalid name for a callback script: afterMigrate__.cypher");
		}

		@Test
		void shouldReplaceUnderScores() throws MalformedURLException {

			CypherBasedCallback callback = new CypherBasedCallback(
					ResourceContext.of(new URL("file:./afterMigrate__insert some_description here.cypher")));
			assertThat(callback.getOptionalDescription()).hasValue("insert some description here");
		}

	}

}
