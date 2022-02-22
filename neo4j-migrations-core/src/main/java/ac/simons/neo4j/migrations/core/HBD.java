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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;

/**
 * Some utilities to deal with Neo4j quirks. Stay away, here be dragons.
 *
 * @author Michael J. Simons
 * @soundtrack Guns n' Roses - Appetite For Democracy 3D
 * @since 1.4.0
 */
final class HBD {

	private static final Set<String> CODES_FOR_EXISTING_CONSTRAINT
		= Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList("Neo.ClientError.Schema.EquivalentSchemaRuleAlreadyExists",
			"Neo.ClientError.Schema.ConstraintAlreadyExists")));

	private static final String CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE = "Neo.ClientError.Schema.ConstraintWithNameAlreadyExists";

	static boolean is4xSeries(ConnectionDetails connectionDetails) {
		return connectionDetails.getServerVersion() != null && connectionDetails.getServerVersion()
			.replaceFirst("(?i)^Neo4j/", "").matches("^4\\.?.*");
	}

	static boolean is44OrHigher(ConnectionDetails connectionDetails) {

		if (connectionDetails.getServerVersion() == null) {
			return false;
		}
		String bare = connectionDetails.getServerVersion().replaceFirst("(?i)^Neo4j/", "");
		Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+).*").matcher(bare);
		if (!matcher.matches()) {
			return false;
		}
		Integer major = Integer.valueOf(matcher.group(1));
		Integer minor = Integer.valueOf(matcher.group(2));
		return major > 4 || (major >= 4 && minor >= 4);
	}

	static Integer silentCreateConstraint(ConnectionDetails connectionDetails, Session session, String statement,
		String name, Supplier<String> failureMessage) {

		String finalStatement;
		String replacement = "";
		if (is4xSeries(connectionDetails) && name != null && !name.trim().isEmpty()) {
			replacement = name.trim() + " ";
		}
		finalStatement = statement.replace("$name ", replacement);

		try {
			return session.writeTransaction(tx -> tx.run(finalStatement).consume().counters().constraintsAdded());
		} catch (Neo4jException e) {

			if (!CODES_FOR_EXISTING_CONSTRAINT.contains(e.code())) {
				throw new MigrationsException(failureMessage.get(), e);
			}
		}

		return 0;
	}

	static Integer silentDropConstraint(ConnectionDetails connectionDetails, Session session, String statement,
		String name) {

		String finalStatement;
		if (is4xSeries(connectionDetails) && name != null && !name.trim().isEmpty()) {
			finalStatement = "DROP CONSTRAINT " + name.trim();
		} else {
			finalStatement = statement;
		}

		try {
			return session.writeTransaction(tx ->
				tx.run(finalStatement).consume().counters().constraintsRemoved());
		} catch (Neo4jException e) {
			if (!"Neo.DatabaseError.Schema.ConstraintDropFailed".equals(e.code())) {
				throw new MigrationsException("Could not remove locks", e);
			}
		}

		return 0;
	}

	static boolean constraintWithNameAlreadyExists(MigrationsException e) {
		return e != null && e.getCause() instanceof ClientException && CONSTRAINT_WITH_NAME_ALREADY_EXISTS_CODE.equals(
			((ClientException) e.getCause()).code());
	}
}
