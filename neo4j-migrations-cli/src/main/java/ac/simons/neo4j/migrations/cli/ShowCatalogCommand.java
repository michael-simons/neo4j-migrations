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

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.catalog.Renderer.Format;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Prints any given catalog.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
@Command(name = "show-catalog",
		description = "Gets the local or remote catalog and prints it to standard out in the given format.")
final class ShowCatalogCommand extends ConnectedCommand {

	@ParentCommand
	private MigrationsCli parent;

	@Option(names = "mode", defaultValue = "REMOTE",
			description = "REMOTE prints the database catalog, LOCAL prints the catalog based on the given migrations.")
	private Mode mode = Mode.REMOTE;

	@Option(names = "format", defaultValue = "XML", description = "The format in which to print the catalog.")
	private Format format = Format.XML;

	@Option(names = "version",
			description = "Which Neo4j version should be assumed for rendering constraints and indexes as Cypher.")
	private String version;

	@Override
	public MigrationsCli getParent() {
		return this.parent;
	}

	@Override
	@SuppressWarnings("squid:S2629") // We definitely want to have the rendered outcome
										// logged and INFO will be enabled.
	Integer withMigrations(Migrations migrations) {

		Renderer<Catalog> renderer = Renderer.get(this.format, Catalog.class);
		Catalog catalog;
		if (this.mode == Mode.LOCAL) {
			catalog = migrations.getLocalCatalog();
		}
		else {
			catalog = migrations.getDatabaseCatalog();
		}

		Neo4jVersion neo4jVersion = (this.version != null) ? Neo4jVersion.of(this.version) : Neo4jVersion.LATEST;
		RenderConfig config = RenderConfig.create()
			.idempotent(neo4jVersion.hasIdempotentOperations())
			.forVersionAndEdition(neo4jVersion, Neo4jEdition.ENTERPRISE);

		MigrationsCli.LOGGER.info(renderer.render(catalog, config).trim());
		return 0;
	}

	enum Mode {

		LOCAL, REMOTE

	}

}
