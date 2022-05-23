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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Gerrit Meier
 */
class IndexToCypherRendererTest {

	// CREATE

	static Stream<Arguments> shouldRenderSimpleIndexCreation() {

		return Stream.of(
			Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX ON :`Person`(`firstname`)"),
			Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX FOR (n:`Person`) ON (n.`firstname`)"),
			Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX FOR (n:`Person`) ON (n.`firstname`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person", Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderSimpleCompositeIndexCreation() {

		return Stream.of(
			Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX ON :`Person`(`age`, `country`)"),
			Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX FOR (n:`Person`) ON (n.`age`, n.`country`)"),
			Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX FOR (n:`Person`) ON (n.`age`, n.`country`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleCompositeIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderNamedIndexCreation() {

		return Stream.of(
			Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX index_name FOR (n:`Person`) ON (n.`age`, n.`country`)"),
			Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX index_name FOR (n:`Person`) ON (n.`age`, n.`country`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderNamedIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderIdempotentIndexCreation() {

		return Stream.of(
			Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE INDEX index_name IF NOT EXISTS FOR (n:`Person`) ON (n.`age`, n.`country`)"),
			Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE INDEX index_name IF NOT EXISTS FOR (n:`Person`) ON (n.`age`, n.`country`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderIdempotentIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailOnIdempotentIndexCreation() {

		return Stream.of(
			Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
			Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnIdempotentIndexCreation(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion");
	}

	// DROP

	static Stream<Arguments> shouldRenderUnnamedIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX ON :`Person`(`firstname`)"),
				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX ON :`Person`(`firstname`)"),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX ON :`Person`(`firstname`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderUnnamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person", Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderUnnamedCompositeIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", false, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX ON :`Person`(`age`, `country`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderUnnamedCompositeIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailUnnamedCompositeIndexDrop() {

		return Stream.of(
				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.DROP, false),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.DROP, false)
		);
	}

	/**
	 * Even though this is just a long existing deprecation, we have decided to fail on dropping unnamed indexes above 3.5.
	 */
	@ParameterizedTest
	@MethodSource
	void shouldFailUnnamedCompositeIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person",
				Arrays.asList("age", "country"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessageStartingWith("Dropping an unnamed index is not supported on Neo4j");
	}

	static Stream<Arguments> shouldRenderNamedIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX ON :`Person`(`firstname`)"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderNamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person", Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderIdempotentNamedIndexDrop() {

		return Stream.of(
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.DROP, true, "DROP INDEX index_name IF EXISTS"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, true, "DROP INDEX index_name IF EXISTS")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderIdempotentNamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person", Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailOnRenderIdempotentNamedIndexDrop() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderIdempotentNamedIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.NODE, "Person", Collections.singleton("firstname"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion");
	}

	static Stream<Arguments> shouldRenderRelationshipIndexCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX index_name FOR ()-[r:`TYPE_NAME`]-() ON (r.`propertyName`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Collections.singleton("propertyName"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderRelationshipCompositeIndexCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE INDEX index_name FOR ()-[r:`TYPE_NAME`]-() ON (r.`propertyName_1`, r.`propertyName_2`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipCompositeIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderRelationshipIdempotentIndexCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE INDEX index_name IF NOT EXISTS FOR ()-[r:`TYPE_NAME`]-() ON (r.`propertyName`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIdempotentIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Collections.singleton("propertyName"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderRelationshipIdempotentCompositeIndexCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE INDEX index_name IF NOT EXISTS FOR ()-[r:`TYPE_NAME`]-() ON (r.`propertyName_1`, r.`propertyName_2`)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIdempotentCompositeIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderRelationshipIndexDrop() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false, "DROP INDEX index_name")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderRelationshipIdempotentIndexDrop() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, true, "DROP INDEX index_name IF EXISTS"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, true, "DROP INDEX index_name IF EXISTS")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderRelationshipIdempotentIndexDrop(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailOnRenderRelationshipIndexCreatePrior43() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderRelationshipIndexCreatePrior43(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessage("The given relationship index cannot be rendered on Neo4j " + serverVersion + ".");
	}

	static Stream<Arguments> shouldFailOnRenderRelationshipIndexDropPrior43() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, true),
				Arguments.of("4.1", false, Neo4jEdition.UNDEFINED, Operator.DROP, false), // ordered entropy on Wish
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.DROP, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderRelationshipIndexDropPrior43(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP, "TYPE_NAME", Arrays.asList("propertyName_1", "propertyName_2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessage("The given relationship index cannot be rendered on Neo4j " + serverVersion + ".");
	}

	static Stream<Arguments> shouldRenderFulltextIndexCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createNodeIndex('index_name',['Movie', 'Book'],['title', 'description'])"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE FULLTEXT INDEX index_name FOR (n:`Movie`|`Book`) ON EACH [n.`title`, n.`description`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE FULLTEXT INDEX index_name FOR (n:`Movie`|`Book`) ON EACH [n.`title`, n.`description`]")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.NODE, "Movie|Book", Arrays.asList("title", "description"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderFulltextIndexIdempotentCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR (n:`Movie`|`Book`) ON EACH [n.`title`, n.`description`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR (n:`Movie`|`Book`) ON EACH [n.`title`, n.`description`]")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextIndexIdempotentCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.NODE, "Movie|Book", Arrays.asList("title", "description"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailOnRenderFulltextIndexIdempotentCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderFulltextIndexIdempotentCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.NODE, "Movie|Book", Arrays.asList("title", "description"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion on");
	}

	static Stream<Arguments> shouldRenderFulltextRelationshipIndexCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CALL db.index.fulltext.createRelationshipIndex('index_name',['TAGGED_AS', 'SOMETHING_ELSE'],['taggedByUser', 'taggedByUser2'])"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE FULLTEXT INDEX index_name FOR ()-[r:`TAGGED_AS`|`SOMETHING_ELSE`]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false, "CREATE FULLTEXT INDEX index_name FOR ()-[r:`TAGGED_AS`|`SOMETHING_ELSE`]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextRelationshipIndexCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.RELATIONSHIP, "TAGGED_AS|SOMETHING_ELSE", Arrays.asList("taggedByUser", "taggedByUser2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderFulltextRelationshipIndexIdempotentCreate() {

		return Stream.of(
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR ()-[r:`TAGGED_AS`|`SOMETHING_ELSE`]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true, "CREATE FULLTEXT INDEX index_name IF NOT EXISTS FOR ()-[r:`TAGGED_AS`|`SOMETHING_ELSE`]-() ON EACH [r.`taggedByUser`, r.`taggedByUser2`]")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderFulltextRelationshipIndexIdempotentCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.RELATIONSHIP, "TAGGED_AS|SOMETHING_ELSE", Arrays.asList("taggedByUser", "taggedByUser2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index, renderConfig)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldFailOnRenderFulltextRelationshipIndexIdempotentCreate() {

		return Stream.of(
				// All versions to be safe
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldFailOnRenderFulltextRelationshipIndexIdempotentCreate(String serverVersion, boolean named, Neo4jEdition edition, Operator operator, boolean idempotent) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Index index = new Index(named ? "index_name" : null, Index.Type.FULLTEXT, TargetEntityType.RELATIONSHIP, "TAGGED_AS|SOMETHING_ELSE", Arrays.asList("taggedByUser", "taggedByUser2"));

		Renderer<Index> renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThatIllegalStateException().isThrownBy(() -> renderer.render(index, renderConfig))
				.withMessageStartingWith("The given index cannot be rendered in an idempotent fashion on");
	}
}
