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
package ac.simons.neo4j.migrations.core;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Discoverer of migrations.
 *
 * @author Michael J. Simons
 * @param <T> The type of things to discover
 * @soundtrack Motörhead - 1916
 * @since 0.0.3
 */
public interface Discoverer<T> {

	/**
	 * Logger used for writing specific discovery information.
	 */
	Logger LOGGER = Logger.getLogger(Discoverer.class.getName());

	/**
	 * Discover migrations within the given context.
	 *
	 * @param context The context of the ongoing migration.
	 * @return A collection of migrations.
	 */
	Collection<T> discover(MigrationContext context);
}
