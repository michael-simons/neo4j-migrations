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

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;

/**
 * An executable Cypher-based resources as a basis for migrations and callbacks
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
final class DefaultCypherResource implements CypherResource {

	private static final Logger LOGGER = Logger.getLogger(DefaultCypherResource.class.getName());
	private static final Predicate<String> NOT_A_SINGLE_COMMENT =
		s -> {
			if (!Strings.isSingleLineComment(s)) {
				if (s.trim().startsWith(Strings.CYPHER_SINGLE_LINE_COMMENT)) {
					return Arrays.stream(s.split(Strings.LINE_DELIMITER))
						.anyMatch(sub -> !Strings.isSingleLineComment(sub));
				}
				return true;
			}
			return false;
		};

	private static final String USE_DATABASE_EXPRESSION =
		"(?i):use +" + Strings.VALID_DATABASE_NAME + "(?:;?(?:" + Strings.LINE_DELIMITER + ")?)?";

	/**
	 * Cypher delimiter
	 */
	private static final String CYPHER_STATEMENT_DELIMITER = ";(?:" + Strings.LINE_DELIMITER + ")";

	private static final Pattern USE_DATABASE_PATTERN = Pattern.compile(USE_DATABASE_EXPRESSION);

	private static final Pattern CALL_PATTERN = Pattern.compile("(?ims)(?<!`)([^`\\s*]\\s*+CALL\\s*\\{.*}\\s*IN\\s+TRANSACTIONS)(?!`)");

	private static final Pattern USING_PERIODIC_PATTERN = Pattern.compile("(?ims)(?<!`)(([^`\\s*]|^)\\s*+USING\\s+PERIODIC\\s+COMMIT\\s+)(?!`)");

	/**
	 * The identifier of this resource.
	 */
	private final String identifier;
	/**
	 * Flag if line feeds should be converted to the system default.
	 */
	private final boolean autocrlf;
	/**
	 * Actual content provider
	 */
	private final Supplier<InputStream> inputStreamSupplier;

	/**
	 * A lazily initialized list of statements, will be initialized with Double-checked locking into an unmodifiable
	 * list, see {@link #readStatements()}.
	 */
	@SuppressWarnings("squid:S3077")
	private volatile List<String> statements;
	private volatile String checksum;

	DefaultCypherResource(String identifier, boolean autocrlf, Supplier<InputStream> inputStreamSupplier) {

		this.identifier = identifier;
		this.autocrlf = autocrlf;
		this.inputStreamSupplier = inputStreamSupplier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getChecksum() {

		String availableChecksum = this.checksum;
		if (availableChecksum == null) {
			synchronized (this) {
				availableChecksum = this.checksum;
				if (availableChecksum == null) {
					this.checksum = computeChecksum();
					availableChecksum = this.checksum;
				}
			}
		}
		return availableChecksum;
	}

	private String computeChecksum() {
		return computeChecksum(getStatements());
	}

	static String computeChecksum(Collection<String> statements) {
		final CRC32 crc32 = new CRC32();

		for (String statement : statements) {
			byte[] bytes = statement.getBytes(Defaults.CYPHER_SCRIPT_ENCODING);
			crc32.update(bytes, 0, bytes.length);
		}
		return Long.toString(crc32.getValue());
	}

	/**
	 * @return The list of statements to apply.
	 */
	List<String> getStatements() {
		return getStatements(null);
	}

	/**
	 * @return A filtered list of statements
	 */
	List<String> getStatements(Predicate<String> filter) {

		List<String> availableStatements = this.statements;
		if (availableStatements == null) {
			synchronized (this) {
				availableStatements = this.statements;
				if (availableStatements == null) {
					this.statements = readStatements();
					availableStatements = this.statements;
				}
			}
		}
		return filter == null ? availableStatements : availableStatements.stream().filter(filter).toList();
	}

	/**
	 * Scans the resource for statements. Statements must be separated by a `;` followed by a newline.
	 *
	 * @return An unmodifiable list of statements contained inside the resource.
	 * @throws MigrationsException in case the script file could not be read
	 */
	private List<String> readStatements() {

		List<String> newStatements = new ArrayList<>();
		try (Scanner scanner = new Scanner(inputStreamSupplier.get(), Defaults.CYPHER_SCRIPT_ENCODING)
			.useDelimiter(CYPHER_STATEMENT_DELIMITER)) {
			while (scanner.hasNext()) {
				String statement = scanner.next().trim().replaceAll(";$", "").trim();
				if (this.autocrlf) {
					statement = statement.replace("\r\n", "\n");
				}
				if (statement.isEmpty()) {
					continue;
				}
				Matcher useMatcher = USE_DATABASE_PATTERN.matcher(statement);
				boolean isMultiLine = statement.contains("\n");
				StringBuffer finalStatement = new StringBuffer();
				if (useMatcher.find()) {
					handleUseStatement(newStatements, useMatcher, finalStatement);
				}
				while (useMatcher.find()) {
					if (isMultiLine) {
						useMatcher.appendTail(finalStatement);
						throw new MigrationsException(
							"Can't switch database inside a statement, offending statement:\n" + finalStatement);
					}
					handleUseStatement(newStatements, useMatcher, finalStatement);
				}
				useMatcher.appendTail(finalStatement);
				if (finalStatement.length() != 0) {
					newStatements.add(finalStatement.toString());
				}
			}
		}

		return Collections.unmodifiableList(newStatements);
	}

	private static void handleUseStatement(List<String> newStatements, Matcher useMatcher, StringBuffer finalStatement) {
		useMatcher.appendReplacement(finalStatement, "");
		newStatements.add(useMatcher.group(0).trim());
	}

	@Override
	public List<String> getExecutableStatements() {
		return getStatements(NOT_A_SINGLE_COMMENT);
	}

	/**
	 * @return A list of surely identifiable single line comments, either "standalone" or before a valid cypher statement
	 */
	@Override
	public List<String> getSingleLineComments() {
		return getStatements()
			.stream()
			.flatMap(DefaultCypherResource::getSingleLineComments)
			.toList();
	}

	static Stream<String> getSingleLineComments(String statement) {
		if (!statement.startsWith(Strings.CYPHER_SINGLE_LINE_COMMENT)) {
			return Stream.empty();
		}
		boolean notAComment;
		Stream.Builder<String> builder = Stream.builder();
		for (String line : statement.split(Strings.LINE_DELIMITER)) {
			line = line.trim();
			notAComment = !line.startsWith(Strings.CYPHER_SINGLE_LINE_COMMENT);
			if (notAComment) {
				break;
			}
			builder.add(line);
		}
		return builder.build();
	}

	static Optional<String> getDatabaseName(String line) {

		Matcher matcher = USE_DATABASE_PATTERN.matcher(line);
		if (matcher.matches()) {
			return Optional.of(matcher.group(1).toLowerCase(Locale.ROOT));
		}
		return Optional.empty();
	}

	static void executeIn(CypherResource cypherResource, MigrationContext context,
		UnaryOperator<SessionConfig.Builder> sessionCustomizer) {

		List<DatabaseAndStatements> statementsByDatabase = groupStatements(cypherResource.getExecutableStatements());

		statementsByDatabase.forEach(databaseAndStatements -> {

			UnaryOperator<SessionConfig.Builder> finalSessionCustomizer =
				databaseAndStatements.database()
					.map(database -> (UnaryOperator<SessionConfig.Builder>) builder -> builder.withDatabase(database))
					.orElse(sessionCustomizer);

			try (Session session = context.getDriver().session(context.getSessionConfig(finalSessionCustomizer))) {

				List<String> executableStatements = databaseAndStatements.statements();

				var statementsNeedingImplicitTransactions = executableStatements.stream()
					.filter(statement -> getTransactionMode(statement) == TransactionMode.IMPLICIT)
					.collect(Collectors.toSet());

				int numberOfStatements;
				MigrationsConfig.TransactionMode transactionMode = context.getConfig().getTransactionMode();
				if (transactionMode == MigrationsConfig.TransactionMode.PER_STATEMENT || !statementsNeedingImplicitTransactions.isEmpty()) {

					LOGGER.log(Level.FINE, "Executing statements contained in script \"{0}\" in separate transactions", cypherResource.getIdentifier());
					numberOfStatements = executeInSeparateTransactions(session, executableStatements, statementsNeedingImplicitTransactions);

				} else if (transactionMode == MigrationsConfig.TransactionMode.PER_MIGRATION) {

					LOGGER.log(Level.FINE, "Executing statements in script \"{0}\" in one transaction", cypherResource.getIdentifier());
					numberOfStatements = session.writeTransaction(t -> {
						int cnt = 0;
						for (String statement : executableStatements) {
							run(t, statement);
							++cnt;
						}
						return cnt;
					});
				} else {
					throw new MigrationsException("Unknown transaction mode " + transactionMode);
				}

				LOGGER.log(Level.FINE, "Executed {0} statements", numberOfStatements);
			}
		});
	}

	private static int executeInSeparateTransactions(Session session, List<String> executableStatements, Set<String> statementsNeedingImplicitTransactions) {

		int numberOfStatements = 0;
		for (String statement : executableStatements) {
			if (statementsNeedingImplicitTransactions.contains(statement)) {
				++numberOfStatements;
				session.run(statement).consume();
			} else {
				numberOfStatements += session.writeTransaction(t -> {
					run(t, statement);
					return 1;
				});
			}
		}
		return numberOfStatements;
	}

	/**
	 * Indicator of the transaction mode to use
	 */
	enum TransactionMode {
		/**
		 * Use managed transactions (aka transaction functions)
		 */
		MANAGED,
		/**
		 * Use implicit transactions (aka auto commit)
		 */
		IMPLICIT
	}

	/**
	 * REturns the transaction mode needed for the query. When in doubt, use a transactional function.
	 * @param statement The query to evaluate
	 * @return The transaction mode
	 */
	static TransactionMode getTransactionMode(String statement) {

		if (CALL_PATTERN.matcher(statement).find() || USING_PERIODIC_PATTERN.matcher(statement).find()) {
			return TransactionMode.IMPLICIT;
		}
		return TransactionMode.MANAGED;
	}

	/**
	 * A record holding together a list of statements and the database in which they should be executed.
	 */
	record DatabaseAndStatements(Optional<String> database, List<String> statements) {

			DatabaseAndStatements {
				statements = List.copyOf(statements);
			}
	}

	/**
	 * Groups statements into a consecutive lists, each entry starting with a new database. Groups are not in the sense
	 * of a GROUP BY as they are not distinct.
	 *
	 * @param statements The list of statements to group, containing use statements
	 * @return An ordered grouped list, with groups not necessary unique
	 */
	static List<DatabaseAndStatements> groupStatements(List<String> statements) {

		List<DatabaseAndStatements> result = new ArrayList<>();
		Optional<String> current = Optional.empty();
		List<String> sublist = new ArrayList<>();
		for (String statement : statements) {
			Optional<String> databaseName = getDatabaseName(statement);
			if (databaseName.isPresent()) { // If empty, it is not a :use statement
				result.add(new DatabaseAndStatements(current, sublist));
				current = databaseName;
				sublist = new ArrayList<>();
				continue;
			}
			sublist.add(statement);
		}
		result.add(new DatabaseAndStatements(current, sublist));
		return result;
	}

	static void run(QueryRunner runner, String statement) {

		LOGGER.log(Level.FINE, "Running {0}", statement);
		ResultSummary resultSummary = runner.run(statement).consume();
		SummaryCounters c = resultSummary.counters();

		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.log(Level.FINEST,
				"nodesCreated: {0}, nodesDeleted: {1}, relationshipsCreated: {2}, relationshipsDeleted: {3}, propertiesSet: {4}, labelsAdded: {5}, labelsRemoved: {6}, indexesAdded: {7}, indexesRemoved: {8}, constraintsAdded: {9}, constraintsRemoved: {10}",
				new Object[] { c.nodesCreated(), c.nodesDeleted(), c.relationshipsCreated(), c.relationshipsDeleted(),
					c.propertiesSet(),
					c.labelsAdded(), c.labelsRemoved(), c.indexesAdded(), c.indexesRemoved(), c.constraintsAdded(),
					c.constraintsRemoved() });
		}
	}
}
