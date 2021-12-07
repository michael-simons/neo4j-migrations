/*
 * Copyright 2020-2021 the original author or authors.
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

/**
 * Default contract for callbacks that might be involved during various phases of the migration lifecycle.
 * This interface might become public as soon as neo4j-migrations supports Java based callbacks, too.
 *
 * @author Michael J. Simons
 * @since TBA
 */
interface Callback {

	/**
	 * @return the phase in which to invoke this callback
	 */
	LifecyclePhase getPhase();

	/**
	 * Invokes this callback with the given context
	 *
	 * @param context The current migration context
	 * @throws MigrationsException Any unexpected exception will be wrapped and rethrown.
	 */
	void invoke(MigrationContext context);
}
