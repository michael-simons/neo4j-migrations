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
package ac.simons.neo4j.migrations.schema;

import java.util.List;

/**
 * A general entity type.
 *
 * @author Michael J. Simons
 * @param <ET> The concrete entity type
 * @soundtrack Kraftklub - KARGO
 * @since TBA
 */
public interface EntityType<ET extends EntityType<ET>> {

	/**
	 * @return the properties of this entity.
	 */
	List<PropertyType<ET>> getProperties();
}
