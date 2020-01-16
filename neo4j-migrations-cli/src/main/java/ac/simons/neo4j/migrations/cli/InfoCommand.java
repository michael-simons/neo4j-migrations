/*
 * Copyright 2020 the original author or authors.
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

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * The info command.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
@Command(name = "info", description = "Retrieves all applied and pending informations, prints them and exits.")
final class InfoCommand implements Callable<Integer> {

	@CommandLine.ParentCommand
	private MigrationsCli migrationsCli;

	@Override
	public Integer call() throws Exception {
		System.out.println("Getting the context " + migrationsCli.getConfig());
		return 0;
	}
}
