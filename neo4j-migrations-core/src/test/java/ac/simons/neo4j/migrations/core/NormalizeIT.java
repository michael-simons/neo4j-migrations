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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Normalize;
import ac.simons.neo4j.migrations.core.refactorings.RefactoringContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NormalizeIT extends AbstractRefactoringsITTestBase {

	@BeforeEach
	void clearDatabase() {

		try (Session session = this.driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherResource.of(ResourceContext.of(RenameIT.class.getResource("/moviegraph/movies.cypher")))
				.getExecutableStatements()
				.forEach(session::run);
			session.run("MATCH (n:Movie {title:'The Matrix'}) SET n.watched = 'ja'").consume();
			session.run("MATCH (n:Movie {title:'The Matrix Reloaded'}) SET n.watched = 'n'").consume();
			session.run("MATCH (n:Movie {title:'The Matrix Revolutions'}) SET n.watched = 'jo'").consume();
			session.run("create (m:Movie {title:'The Matrix Resurrections'}) set m.watched = false").consume();
			session.run(
					"MATCH (m:Movie {title:'The Matrix'}) <-[a:ACTED_IN]- (:Person {name: 'Emil Eifrem'}) SET a.watched = 'Y'")
				.consume();
		}
	}

	@Test
	@EnabledIf("connectionSupportsCallInTx")
	void shouldBeAbleToCreateDefaultPropertiesWithCustomQueryAndBatches() {

		Normalize normalize = Normalize.asBoolean("watched", Arrays.asList("y", "Y"), Arrays.asList(null, "n"))
			.withCustomQuery("MATCH (m:Movie) RETURN m")
			.inBatchesOf(10);

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = normalize.apply(refactoringContext);

			assertThat(counters.propertiesSet()).isEqualTo(39);
			List<Boolean> watched = session.run("MATCH (m:Movie) RETURN DISTINCT m.watched")
				.list(r -> r.get(0).asBoolean());
			assertThat(watched).containsExactly(false);
		}
	}

	@Test
	@EnabledIf("supportsIdentifyingElements")
	void shouldNormalize() {

		Normalize normalize = Normalize.asBoolean("watched", Arrays.asList("ja", "y", "Y"),
				Collections.singletonList("n"));

		try (Session session = this.driver.session()) {
			RefactoringContext refactoringContext = new DefaultRefactoringContext(this.driver::session);
			Counters counters = normalize.apply(refactoringContext);

			assertThat(counters.propertiesSet()).isEqualTo(5);
			List<Boolean> watched = session.run(
					"MATCH (m:Movie) WHERE m.title =~ 'The Matrix.*' RETURN m.watched AS v UNION ALL MATCH (:Person {name: 'Emil Eifrem'}) -[r:ACTED_IN]->() RETURN r.watched AS v")
				.list(r -> {
					Value value = r.get(0);
					if (value.isNull()) {
						return null;
					}
					return value.asBoolean();
				});
			assertThat(watched).hasSize(5).containsOnly(false, true, null);

			assertThat(session.run("MATCH (m:Movie {title: 'The Matrix Resurrections'}) RETURN m.watched")
				.single()
				.get(0)
				.asBoolean()).isFalse();
		}
	}

}
