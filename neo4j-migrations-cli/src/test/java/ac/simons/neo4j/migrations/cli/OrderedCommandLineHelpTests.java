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
package ac.simons.neo4j.migrations.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class OrderedCommandLineHelpTests {

	@Test
	void commandsShouldBeOrdered() {

		CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();
		spec.addSubcommand("C", CommandLine.Model.CommandSpec.create());
		spec.addSubcommand("B", CommandLine.Model.CommandSpec.create());
		spec.addSubcommand("A", CommandLine.Model.CommandSpec.create());

		OrderedCommandLineHelp orderedCommandLineHelp = new OrderedCommandLineHelp(spec,
				new CommandLine.Help.ColorScheme.Builder().build());

		assertThat(orderedCommandLineHelp.subcommands().keySet()).containsExactly("A", "B", "C");
		assertThat(orderedCommandLineHelp.allSubcommands().keySet()).containsExactly("A", "B", "C");
	}

}
