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
package ac.simons.neo4j.migrations.core.schema;

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.nio.charset.StandardCharsets;

/**
 * Generates an {@link Id} for a {@link SchemaItem}.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class GeneratedId implements Id {

	private final String value;

	static Id of(SchemaItem<?> item) {
		return new GeneratedId(item);
	}

	private GeneratedId(SchemaItem<?> forItem) {

		this.value = String.format("%s_%s",
			forItem.getClass().getSimpleName(),
			Strings.MD5.andThen(Strings.BASE64_ENCODING).apply(forItem.toString().getBytes(StandardCharsets.UTF_8))
		);
	}

	@Override
	public String getValue() {
		return value;
	}
}
