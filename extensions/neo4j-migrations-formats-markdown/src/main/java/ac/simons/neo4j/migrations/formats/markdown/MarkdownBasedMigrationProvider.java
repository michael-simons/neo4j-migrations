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
package ac.simons.neo4j.migrations.formats.markdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ac.simons.neo4j.migrations.core.AbstractResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.Ordered;
import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.ResourceContext;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

/**
 * Implementation of a {@link ResourceBasedMigrationProvider} that deals with Markdown
 * files under the extension of {@code .md}.
 *
 * @author Gerrit Meier
 */
public final class MarkdownBasedMigrationProvider extends AbstractResourceBasedMigrationProvider {

	/**
	 * Creates a new instance of this provider. It should not be necessary to call this
	 * directly, it will be done by the service loader.
	 */
	public MarkdownBasedMigrationProvider() {
		super(Ordered.LOWEST_PRECEDENCE, "md", true);
	}

	@Override
	public Collection<Migration> handle(ResourceContext ctx) {

		StringBuilder content = new StringBuilder();
		CharBuffer buffer = CharBuffer.allocate(1024);
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(ctx.openStream(), Defaults.CYPHER_SCRIPT_ENCODING))) {
			while (in.read(buffer) != -1) {
				buffer.flip();
				content.append(buffer);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

		Parser markdownParser = Parser.builder().build();

		Node node = markdownParser.parse(content.toString());
		MigrationsExtractor collector = new MigrationsExtractor(ctx);
		node.accept(collector);

		return collector.migrations;

	}

	private static class MigrationsExtractor extends AbstractVisitor {

		private final List<Migration> migrations = new ArrayList<>();

		private final ResourceContext ctx;

		MigrationsExtractor(ResourceContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void visit(FencedCodeBlock fencedCodeBlock) {
			this.migrations.add(MarkdownBasedMigration.of(this.ctx, fencedCodeBlock));
		}

	}

}
