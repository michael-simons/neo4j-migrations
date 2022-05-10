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
package ac.simons.neo4j.migrations.core.schema;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.core.Migration;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class SchemaBasedMigrationTest {

	@ParameterizedTest
	@ValueSource(strings = { "01.xml", "02.xml", "03.xml" })
	void checksumShouldBeCorrect(String in) throws IOException {
		try (InputStream source = SchemaBasedMigration.class.getResourceAsStream("/xml/identical-migrations/" + in)) {
			Migration schemaBasedMigration = SchemaBasedMigration.of(source);
			assertThat(schemaBasedMigration.getChecksum()).hasValue("562754918");
		}
	}
}
