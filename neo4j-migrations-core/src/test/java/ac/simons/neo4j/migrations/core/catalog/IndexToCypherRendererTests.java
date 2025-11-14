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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class IndexToCypherRendererTests {

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderSimpleIndexCreation() {

		return Stream.of(
				Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX ON :Person(firstname)"),
				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX FOR (n:Person) ON (n.firstname)"),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX FOR (n:Person) ON (n.firstname)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderSimpleCompositeIndexCreation() {

		return Stream.of(
				Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX ON :Person(age, country)"),
				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX FOR (n:Person) ON (n.age, n.country)"),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX FOR (n:Person) ON (n.age, n.country)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderNamedIndexCreation() {

		return Stream.of(
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX index_name FOR (n:Person) ON (n.age, n.country)"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE INDEX index_name FOR (n:Person) ON (n.age, n.country)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderIdempotentIndexCreation() {

		return Stream.of(
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE INDEX index_name IF NOT EXISTS FOR (n:Person) ON (n.age, n.country)"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE INDEX index_name IF NOT EXISTS FOR (n:Person) ON (n.age, n.country)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnIdempotentIndexCreation() {

		return Stream.of(Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderUnnamedIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP INDEX ON :Person(firstname)"),
				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP INDEX ON :Person(firstname)"),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP INDEX ON :Person(firstname)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderUnnamedCompositeIndexDrop() {

		return Stream.of(Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
				"DROP INDEX ON :Person(age, country)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailUnnamedCompositeIndexDrop() {

		return Stream.of(Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.DROP, false),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.DROP, false));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderNamedIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP INDEX ON :Person(firstname)"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderIdempotentNamedIndexDrop() {

		return Stream.of(
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP INDEX index_name IF EXISTS"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP INDEX index_name IF EXISTS"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnRenderIdempotentNamedIndexDrop() {

		return Stream.of(Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, true));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipIndexCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
				"CREATE INDEX index_name FOR ()-[r:TYPE_NAME]-() ON (r.propertyName)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipCompositeIndexCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
				"CREATE INDEX index_name FOR ()-[r:TYPE_NAME]-() ON (r.propertyName_1, r.propertyName_2)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipIdempotentIndexCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
				"CREATE INDEX index_name IF NOT EXISTS FOR ()-[r:TYPE_NAME]-() ON (r.propertyName)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipIdempotentCompositeIndexCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
				"CREATE INDEX index_name IF NOT EXISTS FOR ()-[r:TYPE_NAME]-() ON (r.propertyName_1, r.propertyName_2)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipIndexDrop() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderRelationshipIdempotentIndexDrop() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP INDEX index_name IF EXISTS"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP INDEX index_name IF EXISTS"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnRenderRelationshipIndexCreatePrior43() {

		return Stream.of(Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnRenderRelationshipIndexDropPrior43() {

		return Stream.of(Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.1", false, Neo4jEdition.UNDEFINED, Operator.DROP, false), // ordered
																							// entropy
																							// on
																							// Wish
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.DROP, true));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderFulltextIndexCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE FULLTEXT INDEX index_name FOR (n:Movie|Book) ON EACH [n.`title`, n.`description`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE FULLTEXT INDEX index_name FOR (n:Movie|Book) ON EACH [n.`title`, n.`description`]"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderFulltextIndexDrop() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"CALL db.index.fulltext.drop('index_name')"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"CALL db.index.fulltext.drop('index_name')"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"CALL db.index.fulltext.drop('index_name')"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"CALL db.index.fulltext.drop('index_name')"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderFulltextIndexIdempotentCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
				"CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR (n:Movie|Book) ON EACH [n.`title`, n.`description`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR (n:Movie|Book) ON EACH [n.`title`, n.`description`]"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnRenderFulltextIndexIdempotentCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderFulltextRelationshipIndexCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE FULLTEXT INDEX index_name FOR ()-[r:TAGGED_AS|SOMETHING_ELSE]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE FULLTEXT INDEX index_name FOR ()-[r:TAGGED_AS|SOMETHING_ELSE]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderFulltextRelationshipIndexIdempotentCreate() {

		return Stream.of(Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
				"CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR ()-[r:TAGGED_AS|SOMETHING_ELSE]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR ()-[r:TAGGED_AS|SOMETHING_ELSE]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldFailOnRenderFulltextRelationshipIndexIdempotentCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true));
	}

	@Test
	void shouldEscapeNames() {

		Renderer<Index> indexRenderer = Renderer.get(Renderer.Format.CYPHER, Index.class);

		Index index;
		String cypher;
		index = Index.forNode("Book").named("das' ist` \\u0027ein \\'test").onProperties("isbn");

		RenderConfig create = RenderConfig.create().forVersionAndEdition("4.4", "ENTERPRISE");
		RenderConfig drop = RenderConfig.drop().forVersionAndEdition("4.4", "ENTERPRISE");

		cypher = indexRenderer.render(index, create);
		assertThat(cypher).isEqualTo("CREATE INDEX `das' ist`` 'ein \\'test` FOR (n:Book) ON (n.isbn)");

		cypher = indexRenderer.render(index, drop);
		assertThat(cypher).isEqualTo("DROP INDEX `das' ist`` 'ein \\'test`");

		index = Index.forNode("Book").named("das' ist` \\u0027ein \\'test").fulltext("isbn");
		cypher = indexRenderer.render(index, create);
		assertThat(cypher).isEqualTo("CREATE FULLTEXT INDEX `das' ist`` 'ein \\'test` FOR (n:Book) ON EACH [n.`isbn`]");

		cypher = indexRenderer.render(index, drop);
		assertThat(cypher).isEqualTo("DROP INDEX `das' ist`` 'ein \\'test`");

		create = RenderConfig.create().forVersionAndEdition("4.2", "ENTERPRISE");
		drop = RenderConfig.drop().forVersionAndEdition("4.2", "ENTERPRISE");

		cypher = indexRenderer.render(index, create);
		assertThat(cypher)
			.isEqualTo("CALL db.index.fulltext.createNodeIndex('das\\' ist` \\'ein \\'test',['Book'],['isbn'])");

		cypher = indexRenderer.render(index, drop);
		assertThat(cypher).isEqualTo("CALL db.index.fulltext.drop('das\\' ist` \\'ein \\'test')");
	}

	@ParameterizedTest
	@MethodSource("shouldRenderSimpleIndexCreation")
	@MethodSource("shouldRenderIdempotentNamedIndexDrop")
	@MethodSource("shouldRenderUnnamedIndexDrop")
	void shouldRenderSimpleIndexesProper(String serverVersion, boolean named, Neo4jEdition edition, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource("shouldRenderSimpleCompositeIndexCreation")
	@MethodSource("shouldRenderIdempotentIndexCreation")
	@MethodSource("shouldRenderUnnamedCompositeIndexDrop")
	void shouldRenderCompositeIndexesProper(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@Test
	void shouldOptionallyRenderOptions() {

		RenderConfig renderConfig = RenderConfig.create().forVersionAndEdition("4.4", "ENTERPRISE");
		Index index = new Index("title_idx", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("Book"),
				Collections.singleton("title"),
				"{`indexConfig`: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, `indexProvider`: \"native-btree-1.0\"}");

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo("CREATE INDEX title_idx FOR (n:Book) ON (n.title)");
		assertThat(renderer.render(index,
				renderConfig.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
					@Override
					public boolean includingOptions() {
						return true;
					}
				}))))
			.isEqualTo(
					"CREATE INDEX title_idx FOR (n:Book) ON (n.title) OPTIONS {`indexConfig`: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, `indexProvider`: \"native-btree-1.0\"}");
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderNamedIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnIdempotentIndexCreation(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion");
	}

	// Even though this is just a long existing deprecation,
	// we have decided to fail on dropping unnamed indexes above 3.5.
	@ParameterizedTest
	@MethodSource
	void shouldFailUnnamedCompositeIndexDrop(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessageStartingWith("Dropping an unnamed index is not supported on Neo4j");
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderNamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@Test
	void shouldNotIncludeIndexTypeOnDrop() {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of("4.4"), Neo4jEdition.ENTERPRISE, Operator.DROP,
				true);
		Index index = new Index("index_name", Index.Type.TEXT, TargetEntityType.NODE, Collections.singleton("Person"),
				Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo("DROP INDEX index_name IF EXISTS");
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderIdempotentNamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE,
				Collections.singleton("Person"), Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion");
	}

	@ParameterizedTest
	@MethodSource("shouldRenderRelationshipIndexCreate")
	@MethodSource("shouldRenderRelationshipIdempotentIndexCreate")
	@MethodSource("shouldRenderRelationshipIndexDrop")
	void shouldRenderRelationshipIndexesProper(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
				Collections.singleton("TYPE_NAME"), Collections.singleton("propertyName"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipCompositeIndexCreate(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
				Collections.singleton("TYPE_NAME"), Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIdempotentCompositeIndexCreate(String serverVersion, boolean named,
			Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
				Collections.singleton("TYPE_NAME"), Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIdempotentIndexDrop(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
				Collections.singleton("TYPE_NAME"), Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderRelationshipIndexCreatePrior43(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
				Collections.singleton("TYPE_NAME"), Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessage("The given relationship index cannot be rendered on Neo4j " + serverVersion + ".");
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderRelationshipIndexDropPrior43(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = Index.forRelationship("TYPE_NAME")
			.named(named ? "index_name" : null)
			.onProperties("propertyName_1", "propertyName_2");

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessage("The given relationship index cannot be rendered on Neo4j " + serverVersion + ".");
	}

	@ParameterizedTest
	@MethodSource("shouldRenderFulltextIndexDrop")
	@MethodSource("shouldRenderFulltextIndexIdempotentCreate")
	@MethodSource("shouldRenderFulltextIndexCreate")
	void shouldRenderFullTextIndexesProper(String serverVersion, boolean named, Neo4jEdition edition, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.NODE,
				Arrays.asList("Movie", "Book"), Arrays.asList("title", "description"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderFulltextIndexIdempotentCreate(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.NODE,
				Arrays.asList("Movie", "Book"), Arrays.asList("title", "description"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion on");
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextRelationshipIndexCreate(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = Index.forRelationship("TAGGED_AS", "SOMETHING_ELSE")
			.named(named ? "index_name" : null)
			.fulltext("taggedByUser", "taggedByUser2");

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextRelationshipIndexIdempotentCreate(String serverVersion, boolean named,
			Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.RELATIONSHIP,
				Arrays.asList("TAGGED_AS", "SOMETHING_ELSE"), Arrays.asList("taggedByUser", "taggedByUser2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderFulltextRelationshipIndexIdempotentCreate(String serverVersion, boolean named,
			Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.RELATIONSHIP,
				Arrays.asList("TAGGED_AS", "SOMETHING_ELSE"), Arrays.asList("taggedByUser", "taggedByUser2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(index, renderConfig))
			.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion on");
	}

	@Test
	void shouldEscapeNonValidThings() {

		RenderConfig config = RenderConfig.create().forVersionAndEdition(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE);
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);

		Index index = Index.forNode("Das ist ein Buch").named("book_id_unique").onProperties("person.a,person.b 😱");
		assertThat(renderer.render(index, config))
			.isEqualTo("CREATE INDEX book_id_unique FOR (n:`Das ist ein Buch`) ON (n.`person.a,person.b 😱`)");

		Index indexRel = Index.forRelationship("DAS IST KEIN BUCH")
			.named("book_id_unique")
			.onProperties("person.a,person.b 😱");
		assertThat(renderer.render(indexRel, config))
			.isEqualTo("CREATE INDEX book_id_unique FOR ()-[r:`DAS IST KEIN BUCH`]-() ON (r.`person.a,person.b 😱`)");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = ";", value = {
			"V3_5; CALL db.index.fulltext.createNodeIndex('stupid_stuff',['Hans|Wurst', 'Isst', 'Wurstsalat'],['aProperty'])",
			"V4_4; CREATE FULLTEXT INDEX stupid_stuff FOR (n:`Hans|Wurst`|Isst|Wurstsalat) ON EACH [n.`aProperty`]" })
	void shouldAlsoSupportReallyStupidLabels(Neo4jVersion version, String expected) {

		Index index = Index.forNode("Hans\\|Wurst", "Isst", "Wurstsalat").named("stupid_stuff").fulltext("aProperty");

		RenderConfig config = RenderConfig.create().forVersionAndEdition(version, Neo4jEdition.ENTERPRISE);
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config)).isEqualTo(expected);
	}

	@Test
	void shouldRenderTextIndex() {

		Index index = Index.forNode("Person").named("node_index_name").text("nickname");

		RenderConfig config = RenderConfig.create().forVersionAndEdition(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config))
			.isEqualTo("CREATE TEXT INDEX node_index_name FOR (n:Person) ON (n.nickname)");
	}

	@Test
	void shouldRenderTextIndexForRelationships() {

		Index index = Index.forRelationship("KNOWS").named("rel_index_name").text("interest");

		RenderConfig config = RenderConfig.create().forVersionAndEdition(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config))
			.isEqualTo("CREATE TEXT INDEX rel_index_name FOR ()-[r:KNOWS]-() ON (r.interest)");
	}

	@Test
	void shouldRenderRange() {
		Index index = new Index("n_a_r", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), "{`indexProvider`: 'range-1.0'}");
		RenderConfig config = RenderConfig.create()
			.forVersionAndEdition(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE)
			.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
				@Override
				public boolean useExplicitPropertyIndexType() {
					return true;
				}
			}));
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config)).isEqualTo("CREATE RANGE INDEX n_a_r FOR (n:A) ON (n.a)");
	}

	@Test
	void shouldRenderBtree() {
		Index index = new Index("n_a_b", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), "{`indexProvider`: 'native-btree-1.0'}");
		RenderConfig config = RenderConfig.create()
			.forVersionAndEdition(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE)
			.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
				@Override
				public boolean useExplicitPropertyIndexType() {
					return true;
				}
			}));
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config)).isEqualTo("CREATE BTREE INDEX n_a_b FOR (n:A) ON (n.a)");
	}

	@Test
	void shouldNotRenderExplicitType() {
		Index index = new Index("n_a_n", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), "{`indexProvider`: 'native-btree-1.0'}");
		RenderConfig config = RenderConfig.create().forVersionAndEdition(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config)).isEqualTo("CREATE INDEX n_a_n FOR (n:A) ON (n.a)");
	}

	@Test
	void shouldRenderVectorIndexProper() {
		Index index = new Index("account_name", Index.Type.VECTOR, TargetEntityType.NODE,
				Collections.singleton("Movie"), Collections.singleton("embedding"),
				"{indexConfig: { `vector.dimensions`: 1536, `vector.similarity_function`: 'cosine' }}");
		index.getOptionalOptions().ifPresent(System.out::println);
		RenderConfig config = RenderConfig.create()
			.ifNotExists()
			.forVersionAndEdition(Neo4jVersion.V5, Neo4jEdition.ENTERPRISE);

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, config)).isEqualTo("""
				CREATE VECTOR INDEX account_name IF NOT EXISTS
				FOR (n:Movie)
				ON (n.embedding)
				OPTIONS {indexConfig: {
					`vector.dimensions`: 1536,
					`vector.similarity_function`: 'cosine'
				}}""".replace("\n", " ").replace("\t", ""));
	}

}
