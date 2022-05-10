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

/**
 * Constants used throughout parsing and reading / writing elements and attributes.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class Constants {

	static final String TYPE = "type";
	static final String PROPERTIES = "properties";

	static final String ID = "id";

	static final String CONSTRAINT = "constraint";
	static final String LABEL = "label";
	static final String PROPERTY = "property";
	static final String OPTIONS = "options";
	static final String CATALOG = "catalog";
	static final String INDEXES = "indexes";
	static final String CONSTRAINTS = "constraints";

	private Constants() {
	}
}
