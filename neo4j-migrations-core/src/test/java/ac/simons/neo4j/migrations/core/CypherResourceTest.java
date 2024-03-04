/*
 * Copyright 2020-2024 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class CypherResourceTest {

	@Test
	void builderWithContentShouldWork() {

		String content = ""
			+ "MATCH (n) RETURN count(n) AS n;\n"
			+ "MATCH (n) RETURN count(n) AS n;\n"
			+ "MATCH (n) RETURN count(n) AS n;\n";

		CypherResource resource = CypherResource.withContent(content).identifiedBy("x");

		assertThat(resource.getIdentifier()).isEqualTo("x");
		assertThat(resource.getChecksum()).isEqualTo("1902097523");
		assertThat(resource.getExecutableStatements()).hasSize(3).containsOnly("MATCH (n) RETURN count(n) AS n");
	}

	@Test
	void exceptionsShouldBeRethrown() throws IOException {

		URL url = mock(URL.class);

		IOException cause = new IOException("Boom");
		when(url.openStream()).thenThrow(cause);
		when(url.getPath()).thenReturn("a.cypher");

		CypherResource resource = CypherResource.of(ResourceContext.of(url));

		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(resource::getExecutableStatements)
			.withCause(cause);
	}

	@Test
	void checksForBoot32ClassLoaderShouldWork() throws IOException {

		URL url = mock(URL.class);
		IOException cause = new FileNotFoundException("Boom");
		when(url.openStream()).thenThrow(cause);
		when(url.toString()).thenReturn("jar:file:/target/UberjarWithNestedJar.jar!/BOOT-INF/classes/neo4j/migrations/V010__Test.cypher");
		when(url.getPath()).thenReturn("V010__Test.cypher");

		CypherResource resource = CypherResource.of(ResourceContext.of(url));

		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(resource::getExecutableStatements)
			.withMessage("java.net.MalformedURLException: no !/ in spec"); // Due to the Boot handler not installed in Core tests.
	}
}
