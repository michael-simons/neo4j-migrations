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

import java.util.List;

import ac.simons.neo4j.migrations.core.refactorings.ListToVector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListToVectorIT {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@SuppressWarnings("resource")
	private final Neo4jContainer neo4j = new Neo4jContainer("neo4j:2025.10.1-enterprise")
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	private Driver driver;

	@BeforeAll
	void initDriver() {

		this.neo4j.start();

		Config config = Config.builder().build();
		this.driver = GraphDatabase.driver(this.neo4j.getBoltUrl(),
				AuthTokens.basic("neo4j", this.neo4j.getAdminPassword()), config);
	}

	@BeforeEach
	void setupGraph() {
		this.driver.executableQuery("""
				CYPHER 25
				MATCH (n) DETACH DELETE n
				NEXT
				CREATE (n1:Test {e: 'schrott'})
				CREATE (n2:Test)
				CREATE (n3:Test {id: 'x'})
				CREATE (an:AnotherNode {id: 'y'})
				CREATE (an2:AnotherNode {id: 'y'})
				CREATE (n1)-[r1:FOO {e: 'schrott'}]->(n1)
				CREATE (n1)-[r2:FOO]->(n2)
				CREATE (n1)-[r3:FOO {id: 'x'}]->(n3)
				CREATE (an)-[r4:SELF_REF]->(an)
				WITH n3, r3, an, an2, r4
				CALL db.create.setNodeVectorProperty(n3, 'e', [0.1, 0.2, 0.3])
				CALL db.create.setNodeVectorProperty(an, 'e', [0.1, 0.2, 0.3])
				CALL db.create.setNodeVectorProperty(an, 'embedding', [1, 2, 3])
				CALL db.create.setRelationshipVectorProperty(r3, 'e', [0.1, 0.2, 0.3])
				CALL db.create.setRelationshipVectorProperty(r4, 'e', [0.1, 0.2, 0.3])
				""").execute();
	}

	@Test
	void shouldRefactorFromCatalog() {

		var migrations = new Migrations(MigrationsConfig.builder().build(), this.driver);
		migrations.apply(ListToVectorIT.class.getResource("/listToVector/V001__test.xml"));

		try (var session = this.driver.session()) {
			List<String> result;

			result = session.run("MATCH (n:Test) RETURN collect(valueType(n.e))")
				.single()
				.get(0)
				.asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("NULL", "STRING NOT NULL",
					"VECTOR<FLOAT NOT NULL>(3) NOT NULL");

			result = session.run("MATCH (n:AnotherNode) RETURN collect(valueType(n.e))")
				.single()
				.get(0)
				.asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("NULL", "VECTOR<FLOAT32 NOT NULL>(3) NOT NULL");

			result = session.run("MATCH (n:AnotherNode) RETURN collect(valueType(n.embedding))")
				.single()
				.get(0)
				.asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("NULL", "VECTOR<INTEGER8 NOT NULL>(3) NOT NULL");

			result = session.run("MATCH ()-[r:FOO]->() RETURN collect(valueType(r.e))")
				.single()
				.get(0)
				.asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("NULL", "STRING NOT NULL",
					"VECTOR<FLOAT NOT NULL>(3) NOT NULL");

			result = session.run("MATCH ()-[r:SELF_REF]->() RETURN collect(valueType(r.e))")
				.single()
				.get(0)
				.asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("VECTOR<FLOAT32 NOT NULL>(3) NOT NULL");
		}
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorNodes(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onNodes("Test").withProperty("e").withElementType(elementType);
		applyAndAssertRefactoring(refactoring, "MATCH (n:Test) RETURN collect(valueType(n.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorOnRelationships(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onRelationships("FOO").withProperty("e").withElementType(elementType);
		applyAndAssertRefactoring(refactoring, "MATCH ()-[r:FOO]->() RETURN collect(valueType(r.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorNodesWithCustomQuery(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onNodesMatching("MATCH (theThing {id:'x'}) RETURN theThing")
			.withProperty("e")
			.withElementType(elementType);
		applyAndAssertRefactoring(refactoring, "MATCH (n:Test) RETURN collect(valueType(n.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorOnRelationshipsWithCustomQuery(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onRelationshipsMatching("MATCH ()-[theThing {id:'x'}]->() RETURN theThing")
			.withProperty("e")
			.withElementType(elementType);
		applyAndAssertRefactoring(refactoring, "MATCH ()-[r:FOO]->() RETURN collect(valueType(r.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorNodesInBatches(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onNodes("Test").withProperty("e").withElementType(elementType).inBatchesOf(23);
		applyAndAssertRefactoring(refactoring, "MATCH (n:Test) RETURN collect(valueType(n.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorOnRelationshipsInBatches(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onRelationships("FOO")
			.withProperty("e")
			.withElementType(elementType)
			.inBatchesOf(23);
		applyAndAssertRefactoring(refactoring, "MATCH ()-[r:FOO]->() RETURN collect(valueType(r.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorNodesWithCustomQueryInBatches(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onNodesMatching("MATCH (theThing {id:'x'}) RETURN theThing")
			.withProperty("e")
			.withElementType(elementType)
			.inBatchesOf(3);
		applyAndAssertRefactoring(refactoring, "MATCH (n:Test) RETURN collect(valueType(n.e))", elementType);
	}

	@ParameterizedTest
	@EnumSource(ListToVector.ElementType.class)
	void shouldRefactorOnRelationshipsWithCustomQueryInBatches(ListToVector.ElementType elementType) {
		var refactoring = ListToVector.onRelationshipsMatching("MATCH ()-[theThing {id:'x'}]->() RETURN theThing")
			.withProperty("e")
			.withElementType(elementType)
			.inBatchesOf(3);
		applyAndAssertRefactoring(refactoring, "MATCH ()-[r:FOO]->() RETURN collect(valueType(r.e))", elementType);
	}

	private void applyAndAssertRefactoring(ListToVector refactoring, String s, ListToVector.ElementType elementType) {
		try (Session session = this.driver.session()) {
			var refactoringContext = new DefaultRefactoringContext(this.driver::session);
			var counters = refactoring.apply(refactoringContext);
			var result = session.run(s).single().get(0).asList(Value::asString);
			assertThat(result).containsExactlyInAnyOrder("NULL", "STRING NOT NULL",
					"VECTOR<%s NOT NULL>(3) NOT NULL".formatted(elementType.name()));
			assertThat(counters.propertiesSet()).isEqualTo(1);
		}
	}

}
