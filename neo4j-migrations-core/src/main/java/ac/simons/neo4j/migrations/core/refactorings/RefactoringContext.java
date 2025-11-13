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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.Optional;

import ac.simons.neo4j.migrations.core.Neo4jVersion;
import ac.simons.neo4j.migrations.core.refactorings.QueryRunner.FeatureSet;

/**
 * A wrapper around access to a Neo4j database. It requires an implementation of a
 * simplified {@see org.neo4j.driver.QueryRunner} based on official Neo4j driver types.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
public interface RefactoringContext {

	/**
	 * Looks if the query would potentially return a single, identifiable element that can
	 * be used as a source element in a refactoring. Implementing classes can use EXPLAIN
	 * but also tooling like
	 * <a href="https://github.com/neo4j-contrib/cypher-dsl">Cypher-DSL</a> to evaluate
	 * the query.
	 * @param query the query to evaluate
	 * @return an optional containing the single result of a query if any
	 */
	Optional<String> findSingleResultIdentifier(String query);

	/**
	 * Returns a simplified query runner with the given feature set. If a context cannot
	 * satisfy the feature set it is supposed to throw an
	 * {@link IllegalArgumentException}.
	 * @param featureSet required feature set
	 * @return a query runner supporting the given feature set
	 * @throws IllegalArgumentException in case feature set cannot be satisfied
	 * @throws IllegalStateException in case there is no defined version in this context
	 */
	QueryRunner getQueryRunner(FeatureSet featureSet);

	/**
	 * Escapes the string {@literal potentiallyNonIdentifier} in all cases when it's not a
	 * valid Cypher identifier in the given context.
	 * @param potentiallyNonIdentifier a value to escape, must not be {@literal null} or
	 * blank
	 * @return the sanitized and quoted value or the same value if no change is necessary.
	 * @since 1.11.0
	 */
	default String sanitizeSchemaName(String potentiallyNonIdentifier) {

		return Neo4jVersion.LATEST.sanitizeSchemaName(potentiallyNonIdentifier);
	}

}
