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
package ac.simons.neo4j.migrations.core.catalog;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class CypherRenderingUtilsTests {

	public static final RenderConfig RENDER_CONFIG = RenderConfig.create()
		.forVersionAndEdition("4.4", "ENTERPRISE")
		.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
			@Override
			public boolean includingOptions() {
				return true;
			}
		}));

	@Test
	void shouldAddBraces() throws IOException {
		String options = "notValid: 'i dont care',\nsomethingElse: 'same'";

		try (StringWriter writer = new StringWriter()) {
			Constraint constraint = new Constraint("isbn_unique", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
					Collections.singleton("isbn"), options, null);
			CypherRenderingUtils.renderOptions(constraint, RENDER_CONFIG, writer);
			writer.flush();
			assertThat(writer).hasToString(" OPTIONS {" + options + "}");
		}
	}

	@Test
	void shouldNotAddDoubleBraces() throws IOException {
		String options = " \t\n{  notValid:\n'i dont care' } ";

		try (StringWriter writer = new StringWriter()) {
			Constraint constraint = new Constraint("isbn_unique", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
					Collections.singleton("isbn"), options, null);
			CypherRenderingUtils.renderOptions(constraint, RENDER_CONFIG, writer);
			writer.flush();
			assertThat(writer).hasToString(" OPTIONS " + options.trim());
		}
	}

}
