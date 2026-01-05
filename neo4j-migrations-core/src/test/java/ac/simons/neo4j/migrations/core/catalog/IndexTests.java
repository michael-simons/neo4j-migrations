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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ac.simons.neo4j.migrations.core.MapAccessorAndRecordImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Gerrit Meier
 */
class IndexTests {

	@Test
	void shouldParseSinglePropertyIndex35() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("node_label_property");
		Value tokenNames = Values.value(List.of("token_name"));
		Value properties = Values.value(List.of("properties"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(
				Map.of("indexName", indexName, "type", type, "tokenNames", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[token_name]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("token_name");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("properties");
	}

	@Test
	void shouldParseCompositePropertyIndex35() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("node_label_property");
		Value tokenNames = Values.value(List.of("token_name"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(
				Map.of("indexName", indexName, "type", type, "tokenNames", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[token_name]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("token_name");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldParseMultilabelFulltextIndex35() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("node_fulltext");
		Value tokenNames = Values.value(Arrays.asList("label1", "label2"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(
				Map.of("indexName", indexName, "type", type, "tokenNames", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.FULLTEXT);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[label1, label2]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("label1", "label2");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldParseConstraintIndex35() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("node_unique_property");
		Value tokenNames = Values.value(List.of("label1"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(
				Map.of("indexName", indexName, "type", type, "tokenNames", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.CONSTRAINT_BACKING_INDEX);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[label1]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("label1");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldParseSinglePropertyIndex() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value tokenNames = Values.value(List.of("token_name"));
		Value properties = Values.value(List.of("properties"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "entityType",
				Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[token_name]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("token_name");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("properties");
	}

	@Test
	void shouldParseSingleRelationshipPropertyIndex() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value tokenNames = Values.value(List.of("token_name"));
		Value properties = Values.value(List.of("properties"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "entityType",
				Values.value("RELATIONSHIP"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.RELATIONSHIP);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[token_name]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("token_name");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("properties");
	}

	@Test
	void shouldParseCompositePropertyIndex() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value tokenNames = Values.value(List.of("token_name"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "entityType",
				Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[token_name]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("token_name");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldNotParseMultilabelPropertyIndex() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value tokenNames = Values.value(Arrays.asList("label1", "label2"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		MapAccessorAndRecordImpl row = new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type,
				"entityType", Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties));

		assertThatIllegalArgumentException().isThrownBy(() -> Index.parse(row))
			.withMessage("Multiple labels or types are only allowed to be specified with fulltext indexes.");
	}

	@Test
	void shouldParseMultilabelFulltextIndex() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("FULLTEXT");
		Value tokenNames = Values.value(Arrays.asList("label1", "label2", "Foo|Bar"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "entityType",
				Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.FULLTEXT);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[label1, label2, Foo|Bar]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("label1", "label2", "Foo|Bar");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldParseConstraintBackingIndexes() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value uniqueness = Values.value("UNIQUE");
		Value tokenNames = Values.value(List.of("label1"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index
			.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "uniqueness", uniqueness,
					"entityType", Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.CONSTRAINT_BACKING_INDEX);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[label1]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("label1");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldParseConstraintBackingIndexesNeo5j() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("BTREE");
		Value tokenNames = Values.value(List.of("label1"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(
				Map.of("name", indexName, "type", type, "owningConstraint", Values.value("constraint_name"),
						"entityType", Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties)));

		assertThat(index.getType()).isEqualTo(Index.Type.CONSTRAINT_BACKING_INDEX);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("index_name");
		assertThat(index.getIdentifier()).isEqualTo("[label1]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("label1");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("property1", "property2");
	}

	@Test
	void shouldFailOnMissingFields() {
		Value indexName = Values.value("index_name");
		Value type = Values.value("FULLTEXT");
		Value tokenNames = Values.value(Arrays.asList("label1", "label2"));
		Value properties = Values.value(Arrays.asList("property1", "property2"));

		assertThatIllegalArgumentException()
			.isThrownBy(() -> Index.parse(new MapAccessorAndRecordImpl(
					Map.of("name", indexName, "type", type, "labelsOrTypes", tokenNames, "properties", properties))))
			.withMessage("Required keys are missing in the row describing the index");

	}

	@Test
	void shouldBuildIndexes() {

		Index index = Index.forNode("A").named("a").onProperties("a1");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("A");
		assertThat(index.getType()).isEqualTo(Index.Type.PROPERTY);

		index = Index.forNode("A", "B").named("a").fulltext("a1");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("A", "B");
		assertThat(index.getType()).isEqualTo(Index.Type.FULLTEXT);

		index = Index.forNode("A").named("a").text("a1");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("A");
		assertThat(index.getType()).isEqualTo(Index.Type.TEXT);
	}

	@Test
	void shouldParseVectorIndex() {
		Value indexName = Values.value("account_name");
		Value type = Values.value("VECTOR");
		Value tokenNames = Values.value(List.of("Movie"));
		Value properties = Values.value(List.of("embedding"));

		Index index = Index.parse(new MapAccessorAndRecordImpl(Map.of("name", indexName, "type", type, "entityType",
				Values.value("NODE"), "labelsOrTypes", tokenNames, "properties", properties, "indexProvider",
				Values.value("vector-2.0"), "options", Values.value(Map.of("indexProvider", "vector-2.0", "indexConfig",
						Map.of("vector.dimensions", 1536, "vector.similarity_function", "COSINE"))))));

		assertThat(index.getType()).isEqualTo(Index.Type.VECTOR);
		assertThat(index.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(index.getName().getValue()).isEqualTo("account_name");
		assertThat(index.getIdentifier()).isEqualTo("[Movie]");
		assertThat(index.getDeconstructedIdentifiers()).containsExactly("Movie");
		assertThat(index.getProperties()).containsExactlyInAnyOrder("embedding");
		assertThat(index.getOptionalOptions()).hasValue(
				"""
						{`indexConfig`: {`vector.dimensions`: 1536, `vector.similarity_function`: "COSINE"}, `indexProvider`: "vector-2.0"}""");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{indexConfig: {}, indexProvider: \"range-1.0\"} ",
			"{indexConfig: {}, `indexProvider`: \"range-1.0\"}", "{indexConfig: {},\n `indexProvider`  : 'range-1.0'}",
			"{`indexProvider`  : 'range-1.0'}", "{`indexProvider`  : 'range-1.0', `indexConfig`: {}}" })
	void shouldDetectRangeIndex(String options) {
		Index index;
		index = new Index("n/a", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), options);
		assertThat(index.isRangePropertyIndex()).isTrue();

		index = new Index("n/a", Index.Type.TEXT, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), options);
		assertThat(index.isRangePropertyIndex()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{indexConfig: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, indexProvider: \"native-btree-1.0\"}",
			"{indexConfig: {}, `indexProvider`: \"native-btree-1.0\"}",
			"{indexConfig: {},\n `indexProvider`  : 'native-btree-1.0'}", "{`indexProvider`  : 'native-btree-1.0'}",
			"{`indexProvider`  : 'native-btree-1.0', `indexConfig`: {}}" })
	void shouldDetectBtreeIndex(String options) {
		Index index;
		index = new Index("n/a", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), options);
		assertThat(index.isBtreePropertyIndex()).isTrue();

		index = new Index("n/a", Index.Type.TEXT, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), options);
		assertThat(index.isBtreePropertyIndex()).isFalse();
	}

	@Test
	void shouldIgnoreTextForType() {
		Index index = new Index("n/a", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("A"),
				Collections.singleton("a"), "{indexConfig: {}, indexProvider: \"text-1.0\"}");
		assertThat(index.isRangePropertyIndex()).isFalse();
		assertThat(index.isBtreePropertyIndex()).isFalse();
	}

}
