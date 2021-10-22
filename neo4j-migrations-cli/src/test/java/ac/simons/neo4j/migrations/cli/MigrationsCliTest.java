/*
 * Copyright 2020-2021 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class MigrationsCliTest {

	@Test
	void shouldLogToSysOut() throws UnsupportedEncodingException {
		PrintStream originalSysout = System.out;
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
			System.setOut(ps);
			MigrationsCli.LOGGER.info("Test");
			String result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			Assertions.assertThat(result).isEqualTo("Test" + System.lineSeparator());
		} finally {
			System.setOut(originalSysout);
		}
	}
}
