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
package ac.simons.neo4j.migrations.cli;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Michael J. Simons
 * @since TBA
 */
@Command(name = "show-catalog", description = "Gets the local or remote catalog and prints it to standard out in the given format.")
final class ShowCatalogCommand extends ConnectedCommand {

	enum Mode {
		LOCAL,
		REMOTE
	}

	@ParentCommand
	private MigrationsCli parent;

	@Option(names = "mode",
		defaultValue = "REMOTE",
		description = "REMOTE prints the database catalog, LOCAL prints the catalog based on the given migrations."
	)
	private Mode mode = Mode.REMOTE;

	@Option(names = "format", defaultValue = "XML", description = "The format in which to print the catalog.")
	private Renderer.Format format = Renderer.Format.XML;

	@Option(names = "version", defaultValue = "LATEST", description = "The Neo4j version specific syntax to use when printing the catalog as Cypher.")
	private Neo4jVersion version = Neo4jVersion.LATEST;

	@Override
	public MigrationsCli getParent() {
		return parent;
	}

	@Override
	Integer withMigrations(Migrations migrations) {

		Renderer<Catalog> renderer = Renderer.get(format, Catalog.class);
		Catalog catalog;
		if (mode == Mode.LOCAL) {
			catalog = migrations.getLocalCatalog().getValue();
		} else {
			catalog = migrations.getDatabaseCatalog().getValue();
		}

		try {
			renderer.render(catalog, RenderConfig.create().idempotent(version.hasIdempotentOperations())
				.forVersionAndEdition(version, Neo4jEdition.ENTERPRISE), System.out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return 0;
	}
}
