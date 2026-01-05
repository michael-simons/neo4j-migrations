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
package ac.simons.neo4j.migrations.core;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.Session;

/**
 * A precondition evaluating a query.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.5.0
 */
final class QueryPrecondition extends AbstractPrecondition implements Precondition {

	private static final Pattern CONDITION_PATTERN = Pattern
		.compile("(?i)^.*?(?<database> in (target|schema))? (?<!that )q'(?<query>.++)?$");

	private final Database database;

	private final String query;

	private QueryPrecondition(Type type, String query, Database database) {
		super(type);
		this.query = query;
		this.database = database;
	}

	/**
	 * Checks if the {@code hint} is matched by the {@link #CONDITION_PATTERN} and if so,
	 * tries to build a factory for a corresponding precondition.
	 * @param hint the complete hint
	 * @return a factory for a precondition or an empty optional if this factory doesn't
	 * match the hint
	 */
	static Optional<Function<Type, Precondition>> tryToParse(String hint) {

		Matcher matcher = CONDITION_PATTERN.matcher(hint);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		String query = matcher.group("query");
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"Wrong Cypher precondition %s. Usage: `<assume|assert> [in <target|schema>] q' <cypher statement>`.",
					Precondition.formattedHint(hint)));
		}
		Database database;
		if (matcher.group("database") != null) {
			database = Database.valueOf(matcher.group(2).toUpperCase(Locale.ROOT));
		}
		else {
			database = Database.TARGET;
		}
		return Optional.of(type -> new QueryPrecondition(type, query.trim(), database));
	}

	@Override
	public boolean isMet(MigrationContext migrationContext) {
		try (Session session = (this.database == Database.SCHEMA) ? migrationContext.getSchemaSession()
				: migrationContext.getSession()) {
			return session.executeRead(tx -> tx.run(this.query).single().get(0).asBoolean());
		}
	}

	String getQuery() {
		return this.query;
	}

	Database getDatabase() {
		return this.database;
	}

	@Override
	public String toString() {
		return String.format("// %s in %s q'%s", getType().keyword(), getDatabase(), this.query);
	}

	/**
	 * Enum to specify in which database the query should be executed.
	 */
	enum Database {

		/**
		 * Inside the database being migrated.
		 */
		TARGET,
		/**
		 * Inside the schema database.
		 */
		SCHEMA,

	}

}
