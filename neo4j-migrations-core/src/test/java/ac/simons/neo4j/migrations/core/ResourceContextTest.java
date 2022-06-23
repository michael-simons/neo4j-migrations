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
import static org.mockito.Mockito.when;

import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

/**
 * @author Michael J. Simons
 */
class ResourceContextTest {

	@ParameterizedTest
	@CsvSource(value = { "a.cypher, a.cypher", "foo/bar/a.cypher, a.cypher" })
	void getIdentifierShouldWork(String path, String expected) {

		URL url = Mockito.mock(URL.class);
		when(url.getPath()).thenReturn(path);
		ResourceContext ctx = ResourceContext.of(url, MigrationsConfig.defaultConfig());
		assertThat(ctx.getIdentifier()).isEqualTo(expected);
	}
}
