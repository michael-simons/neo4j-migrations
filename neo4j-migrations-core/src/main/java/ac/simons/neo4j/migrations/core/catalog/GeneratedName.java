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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

/**
 * Generates an {@link Name} for a {@link CatalogItem}.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class GeneratedName implements Name {

	static Name generate(Class<?> classType, ItemType itemType,
		TargetEntity targetEntity, String identifier, Collection<String> properties, String options) {

		String src = String.format("{type=%s, targetEntity=%s, identifier='%s', properties='%s'%s}", itemType,
			targetEntity, identifier, String.join(",", properties),
			options == null ? "" : ", options='" + options + '\'');

		String value = String.format("%s_%s",
			classType.getSimpleName(),
			Strings.MD5.andThen(Strings.BASE64_ENCODING).apply(src.getBytes(StandardCharsets.UTF_8))
		);

		return new GeneratedName(value);
	}

	private final String value;

	GeneratedName(String value) {
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GeneratedName that = (GeneratedName) o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
