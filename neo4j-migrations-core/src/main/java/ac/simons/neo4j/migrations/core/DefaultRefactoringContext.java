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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import ac.simons.neo4j.migrations.core.internal.Strings;
import ac.simons.neo4j.migrations.core.refactorings.QueryRunner;
import ac.simons.neo4j.migrations.core.refactorings.RefactoringContext;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.Plan;
import org.neo4j.driver.summary.ResultSummary;

/**
 * Default implementation of the {@link RefactoringContext} for Neo4j-Migrations.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
final class DefaultRefactoringContext implements RefactoringContext {

	/**
	 * A pattern matching the result producing operator as described in the manual
	 * <a href=
	 * "https://neo4j.com/docs/cypher-manual/current/execution-plans/operators/">about
	 * Operators</a>.
	 */
	static final Pattern OP_PRODUCE_RESULT_PATTERN = Pattern
		.compile("(?i)ProduceResults(@" + Strings.VALID_DATABASE_NAME + ")?");
	static final String KEY_DETAILS = "Details";

	private final Supplier<Session> sessionSupplier;

	private volatile Neo4jVersion version;

	DefaultRefactoringContext(Supplier<Session> sessionSupplier) {
		this(sessionSupplier, null);
	}

	DefaultRefactoringContext(Supplier<Session> sessionSupplier, Neo4jVersion neo4jVersion) {
		this.sessionSupplier = sessionSupplier;
		this.version = neo4jVersion;
	}

	static boolean isProduceResultOperator(Plan plan) {
		return OP_PRODUCE_RESULT_PATTERN.matcher(plan.operatorType()).matches();
	}

	static boolean hasSingleElement(Plan plan) {

		Value details = plan.arguments().getOrDefault(KEY_DETAILS, Values.NULL);
		if (details.isNull()) {
			return false;
		}

		Predicate<String> nullOrBlank = s -> s == null || s.trim().isEmpty();
		Predicate<String> isExactlyDetails = s -> details.asString().equals(s);

		return plan.identifiers().stream().filter(nullOrBlank.negate().and(isExactlyDetails)).count() == 1L;
	}

	/**
	 * Adapts a query to the syntax supported by the underlying database, especially
	 * unifies all id calls to return strings, so you might be extra careful when dealing
	 * with comparison on those.
	 * @param query the query to adapt
	 * @return an adapted query.
	 */
	Query adaptQuery(Query query) {

		if (getVersion().getMajorVersion() >= 5) {
			return query;
		}
		return query.withText(Strings.replaceElementIdCalls(query.text()));
	}

	Neo4jVersion getVersion() {

		Neo4jVersion availableVersion = this.version;
		if (availableVersion == null) {
			synchronized (this) {
				availableVersion = this.version;
				if (availableVersion == null) {
					String value = this.sessionSupplier.get()
						.executeRead(tx -> tx.run(new Query(
								"CALL dbms.components() YIELD name, versions WHERE name = 'Neo4j Kernel' RETURN versions[0] AS version"))
							.single())
						.get("version")
						.asString();
					this.version = Neo4jVersion.of(value);
					availableVersion = this.version;
				}
			}
		}
		return availableVersion;
	}

	@Override
	public QueryRunner getQueryRunner(QueryRunner.FeatureSet featureSet) {

		Neo4jVersion currentVersion = getVersion();
		if (currentVersion == Neo4jVersion.UNDEFINED) {
			throw new IllegalStateException("Cannot apply refactorings with an undefined version");
		}

		boolean supportsBatching = true;
		if (currentVersion != Neo4jVersion.LATEST) {
			Neo4jVersion requiredVersion = Neo4jVersion.of(featureSet.requiredVersion());
			if (requiredVersion == Neo4jVersion.UNDEFINED) {
				throw new IllegalArgumentException("Undefined version requested");
			}
			if (currentVersion.compareTo(requiredVersion) < 0) {
				throw new IllegalArgumentException("Supported version is " + currentVersion
						+ " which is below the required value of " + requiredVersion);
			}
			supportsBatching = currentVersion.compareTo(Neo4jVersion.V4_4) >= 0;
		}

		if (featureSet.hasBatchingSupport() && !supportsBatching) {
			throw new IllegalArgumentException("Batching is supported only with Neo4j >= 4.4");
		}

		return new DefaultQueryRunner(featureSet, this.sessionSupplier.get(),
				featureSet.hasElementIdSupport() ? this::adaptQuery : UnaryOperator.identity());
	}

	@Override
	public Optional<String> findSingleResultIdentifier(String query) {

		try (Session session = this.sessionSupplier.get()) {
			ResultSummary resultSummary = session.executeRead(tx -> tx.run(new Query("EXPLAIN " + query)).consume());
			Plan root = resultSummary.plan();
			if (isProduceResultOperator(root) && hasSingleElement(root)) {
				return Optional.of(root.arguments().get(KEY_DETAILS).asString());
			}
		}

		return Optional.empty();
	}

	@Override
	public String sanitizeSchemaName(String potentiallyNonIdentifier) {
		return this.getVersion().sanitizeSchemaName(potentiallyNonIdentifier);
	}

	static final class DefaultQueryRunner implements QueryRunner {

		private final Session session;

		private final Transaction transaction;

		private final UnaryOperator<Query> filter;

		private final org.neo4j.driver.QueryRunner delegate;

		DefaultQueryRunner(FeatureSet featureSet, Session session, UnaryOperator<Query> filter) {
			this.session = session;
			this.transaction = featureSet.hasBatchingSupport() ? null : session.beginTransaction();
			this.filter = filter;
			this.delegate = (this.transaction != null) ? this.transaction : session;
		}

		@Override
		public Result run(Query query) {
			return this.delegate.run(this.filter.apply(query));
		}

		@Override
		public void close() {
			if (this.transaction != null) {
				this.transaction.commit();
				this.transaction.close();
			}
			this.session.close();
		}

	}

}
