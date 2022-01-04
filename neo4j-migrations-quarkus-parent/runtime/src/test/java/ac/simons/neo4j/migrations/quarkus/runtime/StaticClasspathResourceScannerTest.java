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
package ac.simons.neo4j.migrations.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class StaticClasspathResourceScannerTest {

	@Test
	void scannerShouldWork() {

		var scanner = StaticClasspathResourceScanner.from(List.of("classpath:static"));
		assertThat(scanner.getResources())
			.hasSize(1)
			.first()
			.satisfies(resource -> {
				assertThat(resource.getPath()).isEqualTo("static/arbitrary.cypher");
				assertThat(resource.getUrl()).isNotNull();
			});
	}

	@Test
	void shouldFilterByLocation() {

		var scanner = StaticClasspathResourceScanner.from(List.of("classpath:static", "classpath:also"));
		assertThat(scanner.getResources()).hasSize(2);

		assertThat(scanner.scan(List.of("also"))).hasSize(1);
		assertThat(scanner.scan(List.of("static"))).hasSize(1);

		assertThat(scanner.scan(List.of("static", "also"))).hasSize(2);
		assertThat(scanner.scan(List.of())).isEmpty();
	}

	@Test
	void shouldStripLeadingSlash() {

		var scanner = StaticClasspathResourceScanner.from(List.of("classpath:/static", "classpath:also"));
		assertThat(scanner.getResources()).hasSize(2);

		assertThat(scanner.scan(List.of("also"))).hasSize(1);
		assertThat(scanner.scan(List.of("/static"))).hasSize(1);

		assertThat(scanner.scan(List.of("/static", "also"))).hasSize(2);
		assertThat(scanner.scan(List.of())).isEmpty();
	}
}
