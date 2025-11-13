/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.internal.Neo4jVersionComparator;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;

/**
 * Some utilities to deal with Neo4j quirks. Stay away, here be dragons.
 *
 * @author Michael J. Simons
 * @since 1.4.0
 */
final class HBD {

	private HBD() {
	}

	static boolean is4xSeries(ConnectionDetails connectionDetails) {
		return connectionDetails.getServerVersion() != null
				&& connectionDetails.getServerVersion().replaceFirst("(?i)^Neo4j/", "").matches("^4\\.?.*");
	}

	static boolean is44OrHigher(ConnectionDetails connectionDetails) {

		if (connectionDetails.getServerVersion() == null) {
			return false;
		}

		Neo4jVersionComparator versionComparator = new Neo4jVersionComparator();
		return versionComparator.compare(connectionDetails.getServerVersion(), "4.4") >= 0;
	}

	static Integer silentCreateConstraintOrIndex(ConnectionDetails connectionDetails, Session session, String statement,
			String name, Supplier<String> failureMessage) {

		String finalStatement;
		String replacement = "";
		if (is4xSeries(connectionDetails) && name != null && !name.trim().isEmpty()) {
			replacement = name.trim() + " ";
		}
		finalStatement = statement.replace("$name ", replacement);

		try {
			return session.executeWrite(tx -> tx.run(finalStatement).consume().counters().constraintsAdded());
		}
		catch (Neo4jException ex) {

			if (!Neo4jCodes.CODES_FOR_EXISTING_CONSTRAINT.contains(ex.code())) {
				throw new MigrationsException(failureMessage.get(), ex);
			}
		}

		return 0;
	}

	static Integer silentDropConstraint(ConnectionDetails connectionDetails, Session session, String statement,
			String name) {

		String finalStatement;
		if (is4xSeries(connectionDetails) && name != null && !name.trim().isEmpty()) {
			finalStatement = "DROP CONSTRAINT " + name.trim();
		}
		else {
			finalStatement = statement;
		}

		try {
			return session.executeWrite(tx -> tx.run(finalStatement).consume().counters().constraintsRemoved());
		}
		catch (Neo4jException ex) {
			if (!Neo4jCodes.CONSTRAINT_DROP_FAILED.equals(ex.code())) {
				throw new MigrationsException("Could not remove locks", ex);
			}
		}

		return 0;
	}

	static boolean constraintWithNameAlreadyExists(MigrationsException e) {
		return e != null && e.getCause() instanceof ClientException ce
				&& Neo4jCodes.CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE.equals(ce.code());
	}

	/**
	 * Checks whether the constraint creation might have failed because wrong edition.
	 * @param rawException the exception to check for specific indicators that the wrong
	 * edition has been used
	 * @param connectionDetails the connection details being used
	 * @return {@literal true} when we are relatively sure that a constraint couldn't be
	 * created due to the wrong edition being used
	 */
	static boolean constraintProbablyRequiredEnterpriseEdition(Exception rawException,
			ConnectionDetails connectionDetails) {

		Neo4jException e;
		if (rawException instanceof Neo4jException ne) {
			e = ne;
		}
		else if (rawException.getCause() instanceof Neo4jException ne) {
			e = ne;
		}
		else {
			return false;
		}

		if (!Neo4jCodes.CONSTRAINT_CREATION_FAILED.equals(e.code())) {
			return false;
		}

		if (Neo4jEdition.of(connectionDetails.getServerEdition()) == Neo4jEdition.ENTERPRISE) {
			return false;
		}

		return e.getMessage()
			.toLowerCase(Locale.ROOT)
			.contains("constraint requires Neo4j Enterprise Edition".toLowerCase(Locale.ROOT));
	}

	/**
	 * This will call {@code db.awaitIndexes()} if the result summary from any previous
	 * statement indicates that indexes or constraints have been added.
	 * @param session the session to wait in
	 * @param counters relevant counters from one or more queries
	 */
	static void vladimirAndEstragonMayWait(Session session, Counters counters) {

		if (counters.indexesAdded() == 0 && counters.constraintsAdded() == 0) {
			return;
		}

		Migrations.LOGGER.log(Level.FINE, "Waiting for new indexes to come online.");
		session.run("CALL db.awaitIndexes()").consume();
		Migrations.LOGGER.log(Level.FINE, "Done.");
	}

}
