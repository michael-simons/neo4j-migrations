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

import java.util.Map;
import java.util.TreeMap;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Returns the subcommands in an ordered fashion.
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
final class OrderedCommandLineHelp extends CommandLine.Help {

	OrderedCommandLineHelp(CommandSpec commandSpec, ColorScheme colorScheme) {
		super(commandSpec, colorScheme);
	}

	@Override
	public Map<String, CommandLine.Help> subcommands() {
		return new TreeMap<>(super.subcommands());
	}

	@Override
	public Map<String, CommandLine.Help> allSubcommands() {
		return new TreeMap<>(super.allSubcommands());
	}

}
