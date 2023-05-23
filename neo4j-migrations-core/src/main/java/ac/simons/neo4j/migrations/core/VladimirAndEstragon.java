/*
 * Copyright 2020-2023 the original author or authors.
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

import java.util.logging.Level;

import org.neo4j.driver.Session;

import ac.simons.neo4j.migrations.core.refactorings.Counters;

/**
 * Folks that like to wait.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
final class VladimirAndEstragon {

	/**
	 * This will call {@code db.awaitIndexes()} if the result summary from any previous statement indicates that indexes
	 * or constraints have been added.
	 *
	 * @param session  the session to wait in
	 * @param counters relevant counters from one or more queries
	 */
	static void mayWait(Session session, Counters counters) {

		if (counters.indexesAdded() == 0 && counters.constraintsAdded() == 0) {
			return;
		}

		Migrations.LOGGER.log(Level.FINE, "Waiting for new indexes to come online.");
		session.run("CALL db.awaitIndexes()");
		Migrations.LOGGER.log(Level.FINE, "Done.");
	}
}
