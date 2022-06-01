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

import java.util.Collections;
import java.util.List;

/**
 * The empty catalog.
 *
 * @author Michael J. Simons
 * @soundtrack Carcass - Surgical Steel
 * @since TBA
 */
enum EmptyCatalog implements Catalog {
	INSTANCE;

	@Override
	public List<Constraint> getConstraints() {
		return Collections.emptyList();
	}
}
