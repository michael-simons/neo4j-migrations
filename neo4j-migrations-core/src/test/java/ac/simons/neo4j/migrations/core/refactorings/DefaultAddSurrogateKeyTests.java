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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class DefaultAddSurrogateKeyTests {

	@ParameterizedTest
	@ValueSource(strings = { "foo()", "foo  ( ) ", "fo123(%s, 23)", "a.b.c.whatever( ) " })
	void isReadyMadeFunctionCallShouldDetectThings(String value) {
		assertThat(DefaultAddSurrogateKey.isReadyMadeFunctionCall(value)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "foo", "baz.bar" })
	void shouldNotMatchNames(String value) {
		assertThat(DefaultAddSurrogateKey.isReadyMadeFunctionCall(value)).isFalse();
	}

	@Test
	void shouldSanitizeInputForNodes() {
		DefaultAddSurrogateKey refactoring = (DefaultAddSurrogateKey) AddSurrogateKey.toNodes("Les Node")
			.withProperty("Le` property");
		Query query = refactoring.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
		assertThat(query.text())
			.isEqualTo("MATCH (n:`Les Node`) WHERE n.`Le`` property` IS NULL SET n.`Le`` property` = randomUUID()");
	}

	@Test
	void shouldSanitizeInputForRelationships() {
		DefaultAddSurrogateKey refactoring = (DefaultAddSurrogateKey) AddSurrogateKey.toRelationships("ACTED IN")
			.withProperty("Le` property");
		Query query = refactoring.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
		assertThat(query.text()).isEqualTo(
				"MATCH ()-[r:`ACTED IN`]->() WHERE r.`Le`` property` IS NULL SET r.`Le`` property` = randomUUID()");
	}

	@Nested
	class ToNodes {

		@Test
		void nonErrorCaseShouldWork() {

			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("A");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					assertThat(v.getIdentifiers()).containsExactly("A");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH (n:A) WHERE n.id IS NULL SET n.id = randomUUID()");
				});

			refactoring = AddSurrogateKey.toNodes("A", "B", "B", "C");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					assertThat(v.getIdentifiers()).containsExactly("A", "B", "C");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH (n:A:B:C) WHERE n.id IS NULL SET n.id = randomUUID()");
				});
		}

		@Test
		void shouldPreventNullLabel() {
			assertThatIllegalArgumentException().isThrownBy(() -> AddSurrogateKey.toNodes(null))
				.withMessage("At least one label is required");
		}

		@Test
		void shouldPreventBlankLabels() {
			assertThatIllegalArgumentException().isThrownBy(() -> AddSurrogateKey.toNodes("A", "\t "))
				.withMessage("Empty identifiers cannot be used to identify target nodes");
		}

		@Test
		void withPropertyShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("A").withProperty("foobar");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					assertThat(v.getIdentifiers()).containsExactly("A");
					assertThat(v.getProperty()).isEqualTo("foobar");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text())
						.isEqualTo("MATCH (n:A) WHERE n.foobar IS NULL SET n.foobar = randomUUID()");
				});
		}

		@Test
		void withGeneratorFunctionShouldWork1() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("A").withGeneratorFunction("foobar");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					assertThat(v.getIdentifiers()).containsExactly("A");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("foobar");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH (n:A) WHERE n.id IS NULL SET n.id = foobar()");
				});
		}

		@Test
		void withGeneratorFunctionShouldWork2() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("Movie").withGeneratorFunction("id(%s)");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH (n:Movie) WHERE n.id IS NULL SET n.id = id(n)");
				});
		}

		@Test
		void withGeneratorFunctionShouldWork3() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("Movie").withGeneratorFunction("randomUUID()");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH (n:Movie) WHERE n.id IS NULL SET n.id = randomUUID()");
				});
		}

		@Test
		void withBatchSizeShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("Movie")
				.withGeneratorFunction("randomUUID()")
				.withProperty("theId")
				.inBatchesOf(23);
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo(
							"MATCH (n:Movie) WHERE n.theId IS NULL CALL { WITH n SET n.theId = randomUUID() } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey
				.toNodesMatching("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId");

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz } WITH baz AS n SET n.theId = id(n)");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey
				.toNodesMatching("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId")
				.inBatchesOf(23);

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz } WITH baz AS n CALL { WITH n SET n.theId = id(n) } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork2() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("WILL BE DUMPED")
				.withCustomQuery("MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId")
				.inBatchesOf(23);

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getIdentifiers()).isEmpty();
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) WHERE n.title =~ '.*Matrix.*' RETURN baz } WITH baz AS n CALL { WITH n SET n.theId = id(n) } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork3() {
			AddSurrogateKey refactoring = AddSurrogateKey.toNodes("Movie")
				.withCustomQuery(null)
				.withProperty("foobar")
				.withCustomQuery("");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.NODE);
					assertThat(v.getIdentifiers()).containsExactly("Movie");
					assertThat(v.getProperty()).isEqualTo("foobar");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text())
						.isEqualTo("MATCH (n:Movie) WHERE n.foobar IS NULL SET n.foobar = randomUUID()");
				});
		}

	}

	@Nested
	class ToRelationships {

		@Test
		void nonErrorCaseShouldWork() {

			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("ACTED_IN");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					assertThat(v.getIdentifiers()).containsExactly("ACTED_IN");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text())
						.isEqualTo("MATCH ()-[r:ACTED_IN]->() WHERE r.id IS NULL SET r.id = randomUUID()");
				});
		}

		@Test
		void shouldPreventNullTypes() {
			assertThatIllegalArgumentException().isThrownBy(() -> AddSurrogateKey.toRelationships(null))
				.withMessage("An empty type cannot be used to identify target relationships");
		}

		@Test
		void shouldPreventBlankTypes() {
			assertThatIllegalArgumentException().isThrownBy(() -> AddSurrogateKey.toRelationships("\t "))
				.withMessage("An empty type cannot be used to identify target relationships");
		}

		@Test
		void withPropertyShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("PRODUCED").withProperty("foobar");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					assertThat(v.getIdentifiers()).containsExactly("PRODUCED");
					assertThat(v.getProperty()).isEqualTo("foobar");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text())
						.isEqualTo("MATCH ()-[r:PRODUCED]->() WHERE r.foobar IS NULL SET r.foobar = randomUUID()");
				});
		}

		@Test
		void withGeneratorFunctionShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("DIRECTED").withGeneratorFunction("id(%s)");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					assertThat(v.getIdentifiers()).containsExactly("DIRECTED");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("id(%s)");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo("MATCH ()-[r:DIRECTED]->() WHERE r.id IS NULL SET r.id = id(r)");
				});
		}

		@Test
		void withBatchSizeShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("DIRECTED")
				.withGeneratorFunction("randomUUID()")
				.withProperty("theId")
				.inBatchesOf(23);
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text()).isEqualTo(
							"MATCH ()-[r:DIRECTED]->() WHERE r.theId IS NULL CALL { WITH r SET r.theId = randomUUID() } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey
				.toRelationshipsMatching(
						"MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId");

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS baz } WITH baz AS r SET r.theId = id(r)");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationshipsMatching(
					"MATCH (n:Movie) <-[r:WROTE] -() WHERE n.title =~ '.*Vendetta.*' AND r.theId IS NULL RETURN r AS baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId")
				.inBatchesOf(23);

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) <-[r:WROTE] -() WHERE n.title =~ '.*Vendetta.*' AND r.theId IS NULL RETURN r AS baz } WITH baz AS r CALL { WITH r SET r.theId = id(r) } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork2() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("WILL BE DUMPED")
				.withCustomQuery(
						"MATCH (n:Movie) <-[r:WROTE] -() WHERE n.title =~ '.*Vendetta.*' AND r.theId IS NULL RETURN r AS baz")
				.withGeneratorFunction("id(%s)")
				.withProperty("theId")
				.inBatchesOf(23);

			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getIdentifiers()).isEmpty();
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("baz"));
					assertThat(query.text()).isEqualTo(
							"CALL { MATCH (n:Movie) <-[r:WROTE] -() WHERE n.title =~ '.*Vendetta.*' AND r.theId IS NULL RETURN r AS baz } WITH baz AS r CALL { WITH r SET r.theId = id(r) } IN TRANSACTIONS OF 23 ROWS");
				});
		}

		@Test
		void withCustomQueryAndBatchesShouldWork3() {
			AddSurrogateKey refactoring = AddSurrogateKey.toRelationships("ACTED_IN")
				.withCustomQuery(null)
				.withCustomQuery("");
			assertThat(refactoring).isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(DefaultAddSurrogateKey.class))
				.satisfies(v -> {
					assertThat(v.getTarget()).isEqualTo(DefaultAddSurrogateKey.Target.RELATIONSHIP);
					assertThat(v.getIdentifiers()).containsExactly("ACTED_IN");
					assertThat(v.getProperty()).isEqualTo("id");
					assertThat(v.getGenerator()).isEqualTo("randomUUID");
					Query query = v.generateQuery(Neo4jVersion.LATEST::sanitizeSchemaName, s -> Optional.of("n"));
					assertThat(query.text())
						.isEqualTo("MATCH ()-[r:ACTED_IN]->() WHERE r.id IS NULL SET r.id = randomUUID()");
				});
		}

	}

}
