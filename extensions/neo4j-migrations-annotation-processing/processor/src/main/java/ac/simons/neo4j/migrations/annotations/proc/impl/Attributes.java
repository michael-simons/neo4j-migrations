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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Access to commonly used attribute names and their values.
 *
 * @author Michael J. Simons
 * @since 1.15.0
 */
final class Attributes {

	static final String TYPE = "type";
	static final String VALUE = "value";
	static final String NAME = "name";
	static final String LABEL = "label";
	static final String LABELS = "labels";
	static final String PRIMARY_LABEL = "primaryLabel";
	static final String PROPERTIES = "properties";
	static final String PROPERTY = "property";
	static final String UNIQUE = "unique";
	static final String INDEX_TYPE = "indexType";
	static final String OPTIONS = "options";
	static final String KEY = "key";

	private Attributes() {

	}

	static Optional<ExecutableElement> get(TypeElement annotation, String name) {
		if (annotation == null) {
			return Optional.empty();
		}
		return annotation.getEnclosedElements()
			.stream()
			.filter(e -> e.getSimpleName().contentEquals(name))
			.map(ExecutableElement.class::cast)
			.findFirst();
	}

}
