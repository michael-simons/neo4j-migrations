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
package ac.simons.neo4j.migrations.formats.adoc;

import ac.simons.neo4j.migrations.core.AbstractCypherBasedMigration;
import ac.simons.neo4j.migrations.core.CypherResource;
import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.ResourceContext;

import org.asciidoctor.ast.Block;

/**
 * A Cypher based migration that extract its content from AsciiDoctor source blocks.
 *
 * @author Michael J. Simons
 * @soundtrack Koljah - Aber der Abgrund
 * @since 1.8.0
 */
final class AsciiDoctorBasedMigration extends AbstractCypherBasedMigration {

	static Migration of(ResourceContext ctx, Block block) {
		return new AsciiDoctorBasedMigration(ctx.getIdentifier(), block.getId(), block.getSource());
	}

	private final String document;

	private AsciiDoctorBasedMigration(String document, String id, String content) {
		super(CypherResource.withContent(content).identifiedBy(id));
		this.document = document;
	}

	@Override
	public String getSource() {
		return document + "#" + cypherResource.getIdentifier();
	}
}
