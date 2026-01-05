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
package ac.simons.neo4j.migrations.core.catalog;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.regex.Pattern;

import ac.simons.neo4j.migrations.core.Neo4jVersion;

/**
 * Shared Cypher specific rendering.
 *
 * @author Michael J. Simons
 * @since 1.13.0
 */
final class CypherRenderingUtils {

	private static final Pattern MAP_PATTERN = Pattern.compile("(?s)\\{.+}");

	private CypherRenderingUtils() {
	}

	static void renderOptions(AbstractCatalogItem<?> item, RenderConfig config, Writer writer) throws IOException {
		Optional<String> options = item.getOptionalOptions().map(String::trim);
		Neo4jVersion version = config.getVersion();
		if (config.getOperator() == Operator.CREATE && version.supportsSchemaOptions() && options.isPresent()
				&& config.includeOptions()) {
			boolean hasBraces = MAP_PATTERN.matcher(options.get()).matches();
			if (hasBraces) {
				writer.write(" OPTIONS " + options.get());
			}
			else {
				writer.write(" OPTIONS {" + options.get() + "}");
			}
		}
	}

}
