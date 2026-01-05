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
package ac.simons.neo4j.migrations.annotations.proc;

import java.util.List;

/**
 * The type of an element inside a graph.
 *
 * @param <ET> the concrete element type
 * @author Michael J. Simons
 * @since 1.11.0
 */
@SuppressWarnings("squid:S119") // I like the generic parameter name.
public interface ElementType<ET extends ElementType<ET>> {

	/**
	 * {@return the canonical name of the (Java) type that "owns" this graph element}
	 */
	String getOwningTypeName();

	/**
	 * {@return the properties of this entity}
	 */
	List<PropertyType<ET>> getProperties();

}
