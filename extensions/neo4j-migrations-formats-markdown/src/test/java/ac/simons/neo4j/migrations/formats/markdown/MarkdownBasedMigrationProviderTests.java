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
package ac.simons.neo4j.migrations.formats.markdown;

import java.net.URL;
import java.util.Collection;

import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.ResourceContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownBasedMigrationProviderTests {

	@ParameterizedTest
	@CsvSource({ "/neo4j/markdown-migrations/initial_schema_draft.md, 2",
			"/neo4j/markdown-migrations/more_content.md        , 1" })
	void shouldLoadAllMigrationsPerAdoc(String resource, int numMigrations) {

		MarkdownBasedMigrationProvider provider = new MarkdownBasedMigrationProvider();
		URL url = MarkdownBasedMigrationProviderTests.class.getResource(resource);
		ResourceContext ctx = ResourceContext.of(url, MigrationsConfig.defaultConfig());

		Collection<Migration> migrations = provider.handle(ctx);
		assertThat(migrations).hasSize(numMigrations);
	}

}
