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
package ac.simons.neo4j.migrations.formats.adoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import ac.simons.neo4j.migrations.core.AbstractResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.Defaults;
import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Ordered;
import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.ResourceContext;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.extension.Treeprocessor;

/**
 * Implementation of a {@link ResourceBasedMigrationProvider} that deals with AsciiDoctor
 * files under the extension of {@code .adoc}.
 *
 * @author Michael J. Simons
 * @since 1.8.0
 */
public final class AsciiDoctorBasedMigrationProvider extends AbstractResourceBasedMigrationProvider {

	private static final String INCLUDED_IGNORED_MARKER = "$NEO4J_MIGRATIONS_CHOSE_TO_IGNORE_THIS_INCLUDE$";

	/**
	 * Creates a new instance of this provider. It should not be necessary to call this
	 * directly, it will be done by the service loader.
	 */
	public AsciiDoctorBasedMigrationProvider() {
		super(Ordered.LOWEST_PRECEDENCE, "adoc", true);
	}

	@Override
	public Collection<Migration> handle(ResourceContext ctx) {

		StringBuilder content = new StringBuilder();
		CharBuffer buffer = CharBuffer.allocate(1024);
		try (Asciidoctor asciidoctor = Asciidoctor.Factory.create();
				BufferedReader in = new BufferedReader(
						new InputStreamReader(ctx.openStream(), Defaults.CYPHER_SCRIPT_ENCODING))) {
			while (in.read(buffer) != -1) {
				buffer.flip();
				content.append(buffer);
			}
			asciidoctor.load(content.toString(), Options.builder().build());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

		try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
			MigrationsExtractor collector = new MigrationsExtractor(ctx);
			asciidoctor.javaExtensionRegistry().treeprocessor(collector);

			asciidoctor.javaExtensionRegistry().includeProcessor(new IncludeProcessor() {
				@Override
				public boolean handles(String target) {
					return true;
				}

				@Override
				public void process(Document document, PreprocessorReader reader, String target,
						Map<String, Object> attributes) {
					reader.pushInclude(INCLUDED_IGNORED_MARKER, target, target, 1, attributes);
				}
			});
			asciidoctor.load(content.toString(), Options.builder().build());
			return collector.migrations;
		}
	}

	private static class MigrationsExtractor extends Treeprocessor {

		private final List<Migration> migrations = new ArrayList<>();

		private final ResourceContext ctx;

		MigrationsExtractor(ResourceContext ctx) {
			super(new HashMap<>()); // Must be mutable
			this.ctx = ctx;
		}

		@Override
		public Document process(Document document) {
			Map<Object, Object> selector = Map.of("context", ":listing", "style", "source");
			Predicate<Block> includedBlocks = b -> "cypher".equals(b.getAttribute("language"));
			includedBlocks = includedBlocks.and(b -> MigrationVersion.canParse(b.getId()));
			includedBlocks = includedBlocks.and(b -> !b.getSource().contains(INCLUDED_IGNORED_MARKER));
			document.findBy(selector)
				.stream()
				.map(Block.class::cast)
				.filter(includedBlocks)
				.map(b -> AsciiDoctorBasedMigration.of(this.ctx, b))
				.forEach(this.migrations::add);

			return document;
		}

	}

}
