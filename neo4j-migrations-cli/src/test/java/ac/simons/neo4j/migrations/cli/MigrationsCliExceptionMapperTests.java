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

import ac.simons.neo4j.migrations.core.MigrationsException;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import picocli.CommandLine;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class MigrationsCliExceptionMapperTests {

	@Test
	void serviceUnavailableExceptionShouldBeASoftwareProblem() {

		var result = new MigrationsCliExceptionMapper().getExitCode(new ServiceUnavailableException("n/a"));
		assertThat(result).isEqualTo(CommandLine.ExitCode.SOFTWARE);
	}

	@Test
	void migrationExceptionShouldBeUsage() {

		var result = new MigrationsCliExceptionMapper().getExitCode(new MigrationsException("n/a"));
		assertThat(result).isEqualTo(CommandLine.ExitCode.USAGE);
	}

	@Test
	void otherShouldBeUsageToo() throws Exception {

		String loggedResult = tapSystemOut(() -> {
			var result = new MigrationsCliExceptionMapper().getExitCode(new IllegalArgumentException("n/a"));
			assertThat(result).isEqualTo(CommandLine.ExitCode.USAGE);
			System.out.flush();
		});
		assertThat(loggedResult).isEqualTo("Uncaught exception." + System.lineSeparator());
	}

}
