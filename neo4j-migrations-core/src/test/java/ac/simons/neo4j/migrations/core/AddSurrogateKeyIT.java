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

import ac.simons.neo4j.migrations.core.refactorings.AddSurrogateKey;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.RefactoringContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.driver.Session;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddSurrogateKeyIT extends AbstractRefactoringsITTestBase {

	@BeforeEach
	void clearDatabase() {

		try (Session session = this.driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherResource.of(ResourceContext.of(AddSurrogateKeyIT.class.getResource("/moviegraph/movies.cypher")))
				.getExecutableStatements()
				.forEach(session::run);
		}
	}

	@Test
	void shouldAddIdsToNodes() {

		AddSurrogateKey addSurrogateKey = AddSurrogateKey.toNodes("Movie");

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = addSurrogateKey.apply(refactoringContext);

			assertThat(counters.propertiesSet()).isEqualTo(38);
			Long moviesWithoutIds = session.run("MATCH (n:Movie) WHERE n.id IS NULL return count(*)")
				.single()
				.get(0)
				.asLong();
			assertThat(moviesWithoutIds).isZero();
		}
	}

	@Test
	void shouldAddIdsToRelationships() {

		AddSurrogateKey addSurrogateKey = AddSurrogateKey.toRelationships("WROTE").withProperty("leId");

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = addSurrogateKey.apply(refactoringContext);

			assertThat(counters.propertiesSet()).isEqualTo(10);
			Long moviesWithoutIds = session.run("MATCH () -[r:WROTE] ->() WHERE r.leId IS NULL return count(*)")
				.single()
				.get(0)
				.asLong();
			assertThat(moviesWithoutIds).isZero();
		}
	}

	@Test
	@EnabledIf("customQueriesSupported")
	void shouldSafelyRenamePropertiesWithMultipleCalls() {

		AddSurrogateKey addSurrogateKey = AddSurrogateKey
			.toNodesMatching("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n");

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);

			Counters counters = addSurrogateKey.apply(refactoringContext);
			assertThat(counters.propertiesSet()).isEqualTo(3);

			Long moviesWithoutIds = session
				.run("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' AND n.id IS NULL return count(*)")
				.single()
				.get(0)
				.asLong();
			assertThat(moviesWithoutIds).isZero();
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldRenameLabelsInBatches() {

		AddSurrogateKey addSurrogateKey = AddSurrogateKey
			.toNodesMatching("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN n")
			.inBatchesOf(23);

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);

			Counters counters = addSurrogateKey.apply(refactoringContext);
			assertThat(counters.propertiesSet()).isEqualTo(3);

			Long moviesWithoutIds = session
				.run("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' AND n.id IS NULL return count(*)")
				.single()
				.get(0)
				.asLong();
			assertThat(moviesWithoutIds).isZero();
		}
	}

}
