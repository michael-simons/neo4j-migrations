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
package ac.simons.neo4j.migrations.formats.adoc;

import java.net.MalformedURLException;
import java.net.URL;

import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.ResourceContext;
import org.asciidoctor.ast.Block;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class AsciiDoctorBasedMigrationTests {

	@Test
	void getSourceShouldWork() throws MalformedURLException {

		Block block = mock(Block.class);
		given(block.getId()).willReturn("V010__some_id");
		ResourceContext ctx = ResourceContext.of(new URL("file:///foobar.adoc"), MigrationsConfig.defaultConfig());
		Migration migration = AsciiDoctorBasedMigration.of(ctx, block);
		assertThat(migration.getSource()).isEqualTo("foobar.adoc#V010__some_id");
	}

}
