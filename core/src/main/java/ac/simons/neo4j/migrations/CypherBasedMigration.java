/*
 * Copyright 2020 the original author or authors.
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
package ac.simons.neo4j.migrations;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;

/**
 * A cypher script based migration.
 *
 * @author Michael J. Simons
 */
final class CypherBasedMigration implements Migration {

	private static final Logger LOGGER = Logger.getLogger(CypherBasedMigration.class.getName());

	/**
	 * The URL of the Cypher script.
	 */
	private final URL url;
	/**
	 * The last path element.
	 */
	private final String script;
	private final String description;
	private final MigrationVersion version;

	/**
	 * A lazily initialized list of statements.
	 */
	private volatile List<String> statements;
	private volatile String checksum;

	CypherBasedMigration(URL url) {

		this.url = url;
		String path = this.url.getPath();
		try {
			path = URLDecoder.decode(path, Defaults.DEFAULT_CYPHER_SCRIPT_ENCODING.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Somethings broken: UTF-8 encoding not supported.");
		}
		int lastIndexOf = path.lastIndexOf("/");
		this.script = lastIndexOf < 0 ? path : path.substring(lastIndexOf + 1);
		this.version = MigrationVersion.parse(this.script);
		this.description = this.version.getDescription();
	}

	@Override
	public MigrationVersion getVersion() {
		return version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MigrationType getType() {
		return MigrationType.CYPHER;
	}

	@Override
	public String getSource() {
		return this.script;
	}

	@Override
	public Optional<String> getChecksum() {

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
		return Optional.of(availableChecksum);
	}

	String computeChecksum() {
		final CRC32 crc32 = new CRC32();

		for (String statement : this.getStatements()) {
			crc32.update(statement.getBytes(Defaults.DEFAULT_CYPHER_SCRIPT_ENCODING));
		}
		return Long.toString(crc32.getValue());
	}

	@Override
	public void apply(MigrationContext context) {

		try (Session session = context.getDriver().session(context.getSessionConfig())) {

			int numberOfStatements = 0;
			MigrationsConfig.TransactionMode transactionMode = context.getConfig().getTransactionMode();
			if (transactionMode == MigrationsConfig.TransactionMode.PER_MIGRATION) {

				LOGGER.log(Level.FINE, "Executing migration \"{0}\" in one transaction", getDescription());
				numberOfStatements = session.writeTransaction(t -> {
					int cnt = 0;
					for (String statement : getStatements()) {
						run(t, statement);
						++cnt;
					}
					return cnt;
				});

			} else if (transactionMode == MigrationsConfig.TransactionMode.PER_STATEMENT) {

				LOGGER.log(Level.FINE, "Executing statements contained in migration \"{0}\" in seperate transactions",
					getDescription());
				for (String statement : getStatements()) {
					numberOfStatements += session.writeTransaction(t -> {
						run(t, statement);
						return 1;
					});
				}
			} else {
				throw new MigrationsException("Unknown transaction mode " + transactionMode);
			}

			LOGGER.log(Level.FINE, "Executed {0} statements", numberOfStatements);
		}
	}

	private void run(QueryRunner runner, String statement) {

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

	/**
	 * @return The list of statements to apply.
	 */
	List<String> getStatements() {

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
		return availableStatements;
	}

	/**
	 * Scans the resource for statements. Statements must be separated by a `;` followed by a newline.
	 *
	 * @return An unmodifiable list of statements contained inside the resource.
	 * @throws IOException
	 */
	private List<String> readStatements() {

		List<String> newStatements = new ArrayList<>();
		try (Scanner scanner = new Scanner(url.openStream(), Defaults.DEFAULT_CYPHER_SCRIPT_ENCODING.name())
			.useDelimiter(Defaults.CYPHER_STATEMENT_DELIMITER)) {
			while (scanner.hasNext()) {

				String statement = scanner.next().trim().replaceAll(";$", "").trim();
				if (statement.isEmpty()) {
					continue;
				}
				newStatements.add(statement);
			}
		} catch (IOException e) {
			throw new MigrationsException("Could not read script file " + this.url, e);
		}

		return Collections.unmodifiableList(newStatements);
	}

	@Override
	public String toString() {
		return "CypherBasedMigration{" +
			"url=" + url +
			", script='" + script + '\'' +
			'}';
	}
}
