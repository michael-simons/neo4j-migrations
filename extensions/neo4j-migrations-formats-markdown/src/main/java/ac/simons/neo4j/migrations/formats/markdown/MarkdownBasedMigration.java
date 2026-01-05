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
package ac.simons.neo4j.migrations.formats.markdown;

import ac.simons.neo4j.migrations.core.AbstractCypherBasedMigration;
import ac.simons.neo4j.migrations.core.CypherResource;
import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.ResourceContext;
import org.commonmark.node.FencedCodeBlock;

/**
 * A Cypher based migration that extract its content from Markdown fenced code blocks.
 *
 * @author Gerrit Meier
 */
final class MarkdownBasedMigration extends AbstractCypherBasedMigration {

	private final String document;

	private MarkdownBasedMigration(String document, String id, String content) {
		super(CypherResource.withContent(content).identifiedBy(id));
		this.document = document;
	}

	static Migration of(ResourceContext ctx, FencedCodeBlock codeBlock) {
		return new MarkdownBasedMigration(ctx.getIdentifier(), extractId(codeBlock.getInfo()), codeBlock.getLiteral());
	}

	private static String extractId(String info) {
		return info.replace("id=", "");
	}

	@Override
	public String getSource() {
		return this.document + "#" + this.cypherResource.getIdentifier();
	}

}
