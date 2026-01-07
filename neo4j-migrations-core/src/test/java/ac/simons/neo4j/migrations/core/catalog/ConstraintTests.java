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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.MapAccessorAndRecordImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class ConstraintTests {

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldParseUniqueNode() {
		return Stream.of(Arguments.of(null, "CONSTRAINT ON ( book:Book ) ASSERT book.isbn IS UNIQUE"),
				Arguments.of("a_name", "CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE"),
				Arguments.of("stupid_stuff",
						"CONSTRAINT ON ( book:Book ) ASSERT (book.fünny things are fünny \uD83D\uDE31. Wow.) IS UNIQUE"),
				Arguments.of("a_name", "CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE"));
	}

	static Stream<Arguments> shouldParseSimpleNodePropertyExistenceConstraint() {
		return Stream.of(Arguments.of(null, "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)"),
				Arguments.of("a_name", "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)"),
				Arguments.of("stupid_stuff",
						"CONSTRAINT ON ( book:Book ) ASSERT exists(book.fünny things are fünny and why not, add more fun. Wow \uD83D\uDE31)"),
				Arguments.of("a_name", "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)"));
	}

	static Stream<Arguments> shouldParseNodeKeyConstraint() {
		return Stream.of(
				Arguments.of(null,
						"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY"),
				Arguments.of("a_name",
						"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY"),
				Arguments.of("stupid_stuff",
						"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname, person.person.whatever, person.person.a,person.b) IS NODE KEY"),
				Arguments.of("constraint_name1",
						"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY"));
	}

	static Stream<Arguments> shouldParseSimpleRelPropertyExistenceConstraint() {
		return Stream.of(Arguments.of("3.5", null, "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"),
				Arguments.of("3.5", "stupid_stuff",
						"CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.x,liked.y)"),
				Arguments.of("4.0", "constraint_name", "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"),
				Arguments.of("4.1", "constraint_name", "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"));
	}

	static Stream<Arguments> showConstraints44() {
		String v = "4.4";

		return Stream.of(Arguments.of(v, Constraint.Type.UNIQUE, "Book", TargetEntityType.NODE,
				Collections.singletonList("title"),
				Map.of("name", Values.value("constraint_name"), "type", Values.value("UNIQUENESS"), "entityType",
						Values.value("NODE"), "labelsOrTypes", Values.value(Collections.singletonList("Book")),
						"properties", Values.value(Collections.singletonList("title")))),
				Arguments.of(v, Constraint.Type.KEY, "Person", TargetEntityType.NODE,
						Arrays.asList("firstname", "surname"),
						Map.of("name", Values.value("constraint_name"), "type", Values.value("NODE_KEY"), "entityType",
								Values.value("NODE"), "labelsOrTypes",
								Values.value(Collections.singletonList("Person")), "properties",
								Values.value(Arrays.asList("firstname", "surname")))),
				Arguments.of(v, Constraint.Type.UNIQUE, "Book", TargetEntityType.NODE, Arrays.asList("a", "b"),
						Map.of("name", Values.value("constraint_name"), "type", Values.value("UNIQUENESS"),
								"entityType", Values.value("NODE"), "labelsOrTypes",
								Values.value(Collections.singletonList("Book")), "properties",
								Values.value(Arrays.asList("a", "b")))),
				Arguments.of(v, Constraint.Type.EXISTS, "Book", TargetEntityType.NODE,
						Collections.singletonList("isbn"),
						Map.of("name", Values.value("constraint_name"), "type", Values.value("NODE_PROPERTY_EXISTENCE"),
								"entityType", Values.value("NODE"), "labelsOrTypes",
								Values.value(Collections.singletonList("Book")), "properties",
								Values.value(Collections.singletonList("isbn")))),
				Arguments.of(v, Constraint.Type.EXISTS, "LIKED", TargetEntityType.RELATIONSHIP,
						Collections.singletonList("day"),
						Map.of("name", Values.value("constraint_name"), "type",
								Values.value("RELATIONSHIP_PROPERTY_EXISTENCE"), "entityType",
								Values.value("RELATIONSHIP"), "labelsOrTypes",
								Values.value(Collections.singletonList("LIKED")), "properties",
								Values.value(Collections.singletonList("day")))),
				Arguments.of(v, Constraint.Type.UNIQUE, "Book", TargetEntityType.NODE,
						Collections.singletonList("isbn"),
						Map.of("name", Values.value("constraint_name"), "type", Values.value("UNIQUENESS"),
								"entityType", Values.value("NODE"), "labelsOrTypes",
								Values.value(Collections.singletonList("Book")), "properties",
								Values.value(Collections.singletonList("isbn")))));
	}

	static Stream<Arguments> shouldDealWithShowConstraints() {

		return Stream.concat(Stream.of("4.2", "4.3")
			.flatMap(v -> Stream.of(Arguments.of(v, Constraint.Type.KEY, "Person", TargetEntityType.NODE,
					Arrays.asList("firstname", "surname"),
					Map.of("name", Values.value("constraint_name"), "type", Values.value("NODE_KEY"), "entityType",
							Values.value("NODE"), "labelsOrTypes", Values.value(Collections.singletonList("Person")),
							"properties", Values.value(Arrays.asList("firstname", "surname")))),
					Arguments.of(v, Constraint.Type.EXISTS, "Book", TargetEntityType.NODE,
							Collections.singletonList("isbn"),
							Map.of("name", Values.value("constraint_name"), "type",
									Values.value("NODE_PROPERTY_EXISTENCE"), "entityType", Values.value("NODE"),
									"labelsOrTypes", Values.value(Collections.singletonList("Book")), "properties",
									Values.value(Collections.singletonList("isbn")))),
					Arguments.of(v, Constraint.Type.EXISTS, "LIKED", TargetEntityType.RELATIONSHIP,
							Collections.singletonList("day"),
							Map.of("name", Values.value("constraint_name"), "type",
									Values.value("RELATIONSHIP_PROPERTY_EXISTENCE"), "entityType",
									Values.value("RELATIONSHIP"), "labelsOrTypes",
									Values.value(Collections.singletonList("LIKED")), "properties",
									Values.value(Collections.singletonList("day")))),
					Arguments.of(v, Constraint.Type.UNIQUE, "Book", TargetEntityType.NODE,
							Collections.singletonList("isbn"),
							Map.of("name", Values.value("constraint_name"), "type", Values.value("UNIQUENESS"),
									"entityType", Values.value("NODE"), "labelsOrTypes",
									Values.value(Collections.singletonList("Book")), "properties",
									Values.value(Collections.singletonList("isbn")))))),
				showConstraints44());
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseUniqueNode(String name, String description) {

		Constraint constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("name",
				(name != null) ? Values.value(name) : Values.NULL, "description", Values.value(description))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.UNIQUE);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("fünny things are fünny 😱. Wow.");
		}
		else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			}
			else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}

			assertThat(constraint.getProperties()).containsExactly("isbn");
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseSimpleNodePropertyExistenceConstraint(String name, String description) {

		Constraint constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("name",
				(name != null) ? Values.value(name) : Values.NULL, "description", Values.value(description))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.EXISTS);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties())
				.containsExactly("fünny things are fünny and why not, add more fun. Wow 😱");
		}
		else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			}
			else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("isbn");
		}
	}

	@Test // GH-1011
	void shouldParseNodePropertyTypeConstraint() {

		var constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("entityType", Values.value("NODE"),
				"name", Values.value("movie_title"), "labelsOrTypes", Values.value(List.of("Movie")), "type",
				Values.value("NODE_PROPERTY_TYPE"), "properties", Values.value(List.of("release_date")), "propertyType",
				Values.value("LOCAL DATETiME"), "createStatement",
				Values.value("CREATE CONSTRAINT `movie_title` FOR (n:`Movie`) REQUIRE (n.`title`) IS :: STRING"))));

		assertThat(constraint.getName()).extracting(Name::getValue).isEqualTo("movie_title");
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.PROPERTY_TYPE);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Movie");
		assertThat(constraint.getProperties()).containsExactly("release_date");
		assertThat(constraint.getPropertyType()).isEqualTo(PropertyType.LOCAL_DATETIME);
	}

	@Test // GH-1011
	void shouldParseRelPropertyTypeConstraint() {

		var constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("entityType",
				Values.value("RELATIONSHIP"), "name", Values.value("part_of"), "labelsOrTypes",
				Values.value(List.of("PART_OF")), "type", Values.value("RELATIONSHIP_PROPERTY_TYPE"), "properties",
				Values.value(List.of("order")), "propertyType", Values.value("INTEGER"), "createStatement", Values
					.value("CREATE CONSTRAINT `part_of` FOR ()-[r:`PART_OF`]-() REQUIRE (r.`order`) IS :: INTEGER"))));

		assertThat(constraint.getName()).extracting(Name::getValue).isEqualTo("part_of");
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.PROPERTY_TYPE);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.RELATIONSHIP);
		assertThat(constraint.getIdentifier()).isEqualTo("PART_OF");
		assertThat(constraint.getProperties()).containsExactly("order");
		assertThat(constraint.getPropertyType()).isEqualTo(PropertyType.INTEGER);
	}

	@ParameterizedTest
	@ValueSource(strings = { "RELATIONSHIP_UNIQUENESS", "RELATIONSHIP_PROPERTY_UNIQUENESS" })
	void shouldParseRelUniquePropertyConstraint(String type) {

		var constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("entityType",
				Values.value("RELATIONSHIP"), "name", Values.value("whatever"), "labelsOrTypes",
				Values.value(List.of("WHATEVER")), "type", Values.value(type), "properties", Values.value(List.of("p")),
				"propertyType", Values.NULL, "createStatement",
				Values.value("CREATE CONSTRAINT `whatever` FOR ()-[r:`WHATEVER`]-() REQUIRE (r.`p`) IS UNIQUE"))));

		assertThat(constraint.getName()).extracting(Name::getValue).isEqualTo("whatever");
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.UNIQUE_RELATIONSHIP_PROPERTY);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.RELATIONSHIP);
		assertThat(constraint.getIdentifier()).isEqualTo("WHATEVER");
		assertThat(constraint.getProperties()).containsExactly("p");
		assertThat(constraint.getPropertyType()).isNull();
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseNodeKeyConstraint(String name, String description) {

		Constraint constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("name",
				(name != null) ? Values.value(name) : Values.NULL, "description", Values.value(description))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.KEY);
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Person");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("firstname", "surname", "person.whatever",
					"person.a,person.b");
		}
		else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			}
			else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("firstname", "surname");
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseSimpleRelPropertyExistenceConstraint(String version, String name, String description) {

		Constraint constraint = Constraint.parse(new MapAccessorAndRecordImpl(Map.of("name",
				(name != null) ? Values.value(name) : Values.NULL, "description", Values.value(description))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.EXISTS);
		assertThat(constraint.getIdentifier()).isEqualTo("LIKED");
		assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.RELATIONSHIP);
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("x,liked.y");
		}
		else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			}
			else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("day");
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldDealWithShowConstraints(String version, Constraint.Type expectedType, String expectedIdentifier,
			TargetEntityType expectedTarget, Collection<String> expectedProperties, Map<String, Value> content) {

		MapAccessor row = new MapAccessorAndRecordImpl(content);
		Constraint constraint = Constraint.parse(row);

		assertThat(constraint.getType()).isEqualTo(expectedType);
		assertThat(constraint.getIdentifier()).isEqualTo(expectedIdentifier);
		assertThat(constraint.getTargetEntityType()).isEqualTo(expectedTarget);
		assertThat(constraint.getProperties()).containsExactlyElementsOf(expectedProperties);
		assertThat(constraint.getName()).isEqualTo(Name.of("constraint_name"));
	}

	@Nested
	class Names {

		@Test
		void defaultNameShouldWork() {

			Constraint constraint = Constraint.forNode("Book").named("book_id_unique").unique("id");
			assertThat(constraint.hasGeneratedName()).isFalse();
		}

		@Test
		void generatedNameShouldBeIdentifiable() {

			Constraint constraint = new Constraint(Constraint.Type.KEY, TargetEntityType.NODE, "Person",
					Arrays.asList("firstname", "surname"), null);
			assertThat(constraint.hasGeneratedName()).isTrue();
		}

	}

	@Nested
	class Builder {

		@ParameterizedTest
		@EnumSource(value = Constraint.Type.class, mode = EnumSource.Mode.EXCLUDE,
				names = "UNIQUE_RELATIONSHIP_PROPERTY")
		void nodeConstraintBuilderShouldWork(Constraint.Type type) {

			Constraint constraint = switch (type) {
				case UNIQUE -> Constraint.forNode("Book").named("foo").unique("bar");
				case UNIQUE_RELATIONSHIP_PROPERTY -> throw new UnsupportedOperationException();
				case EXISTS -> Constraint.forNode("Book").named("foo").exists("bar");
				case KEY -> Constraint.forNode("Book").named("foo").key("bar");
				case PROPERTY_TYPE -> Constraint.forNode("Book").named("foo").type("bar", PropertyType.INTEGER);
			};

			assertThat(constraint.getIdentifier()).isEqualTo("Book");
			assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.NODE);
			assertThat(constraint.getProperties()).containsExactly("bar");
			assertThat(constraint.getType()).isEqualTo(type);
			assertThat(constraint.getName()).isEqualTo(Name.of("foo"));
			if (type == Constraint.Type.PROPERTY_TYPE) {
				assertThat(constraint.getPropertyType()).isEqualTo(PropertyType.INTEGER);
			}
			else {
				assertThat(constraint.getPropertyType()).isNull();
			}
		}

		@Test
		void relConstraintBuilderShouldWork() {

			var constraint = Constraint.forRelationship("Book").named("foo").unique("bar");
			assertThat(constraint.getIdentifier()).isEqualTo("Book");
			assertThat(constraint.getTargetEntityType()).isEqualTo(TargetEntityType.RELATIONSHIP);
			assertThat(constraint.getProperties()).containsExactly("bar");
			assertThat(constraint.getType()).isEqualTo(Constraint.Type.UNIQUE_RELATIONSHIP_PROPERTY);
			assertThat(constraint.getName()).isEqualTo(Name.of("foo"));
			assertThat(constraint.getPropertyType()).isNull();
		}

	}

	@Nested
	class Invalid {

		@Test // GH-1011
		void shouldCheckTypeCombo1() {

			var properties = Collections.singleton("x");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> new Constraint("foo", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Movie",
						properties, PropertyType.DATE))
				.withMessage("A property type can only be used with a property type constraint.");
		}

		@Test // GH-1011
		void shouldCheckTypeCombo2() {

			var properties = Collections.singleton("x");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> new Constraint("foo", Constraint.Type.PROPERTY_TYPE, TargetEntityType.NODE, "Movie",
						properties, null))
				.withMessage("A property type constraint requires a property type.");
		}

		@Test // GH-1011
		void shouldCheckTypeCombo3() {

			var properties = Set.of("x", "y");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> new Constraint("foo", Constraint.Type.PROPERTY_TYPE, TargetEntityType.NODE, "Movie",
						properties, PropertyType.DATE))
				.withMessage("A property type constraint can only be applied to a single property.");
		}

		@Test
		void keyConstraintsShouldNotBeSupportedOnRelationships() {
			List<String> properties = Collections.singletonList("x");
			assertThatIllegalArgumentException().isThrownBy(
					() -> new Constraint(Constraint.Type.KEY, TargetEntityType.RELATIONSHIP, "LIKES", properties, null))
				.withMessage("Key constraints are only supported for nodes, not for relationships.");
		}

	}

	@Nested
	class Equivalence {

		Constraint uniqueBookIdV1 = Constraint.forNode("Book").named("book_id_unique").unique("id");

		@Test
		void shouldNotBeEquivalentToOtherThings() {

			assertThat(this.uniqueBookIdV1.isEquivalentTo(new AbstractCatalogItem<>("book_id_unique",
					Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book", Collections.singletonList("id"), null) {
				@Override
				public boolean isEquivalentTo(CatalogItem<?> that) {
					return false;
				}
			})).isFalse();
		}

		@Test
		void shouldNotBeEquivalentToSame() {

			assertThat(this.uniqueBookIdV1.isEquivalentTo(this.uniqueBookIdV1)).isTrue();
		}

		@Test
		void sameTypeIsRequired() {

			assertThat(
					this.uniqueBookIdV1.isEquivalentTo(Constraint.forNode("Book").named("book_id_unique").exists("id")))
				.isFalse();
		}

		@Test
		void sameEntityIsRequired() {

			Constraint c1 = Constraint.forNode("Book").named("name_exists").exists("name");

			Constraint c2 = Constraint.forRelationship("Book").named("name_exists").exists("name");

			assertThat(c1.isEquivalentTo(c2)).isFalse();
		}

		@Test
		void sameIdentifierIsRequired() {

			assertThat(this.uniqueBookIdV1
				.isEquivalentTo(Constraint.forNode("SomethingElse").named("book_id_unique").unique("id"))).isFalse();
		}

		@Test // GH-656
		void sameOptionsAreNotRequired() {

			Constraint other = new Constraint(null, Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
					Collections.singleton("id"), "foo", null);
			assertThat(this.uniqueBookIdV1.isEquivalentTo(other)).isTrue();
		}

		@Test
		void nullOptionsShouldBeSame() {

			Constraint other = new Constraint(null, Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
					Collections.singleton("id"), " ", null);
			assertThat(this.uniqueBookIdV1.isEquivalentTo(other)).isTrue();
		}

		@Test
		void samePropertiesAreRequired() {

			assertThat(this.uniqueBookIdV1
				.isEquivalentTo(Constraint.forNode("Book").named("book_id_unique").unique("ids"))).isFalse();
		}

		@Test
		void nameIsIrrelevant() {

			assertThat(this.uniqueBookIdV1.isEquivalentTo(Constraint.forNode("Book").named("foo").unique("id")))
				.isTrue();
		}

		@Test
		void allFieldsAndTheNameShouldWorkToo() {

			assertThat(
					this.uniqueBookIdV1.isEquivalentTo(Constraint.forNode("Book").named("book_id_unique").unique("id")))
				.isTrue();
		}

	}

	@Nested
	class Wither {

		@Test
		void shouldReturnSameOnUnchangedOptions() {

			Constraint constraint = Constraint.forNode("X").named("x").unique("x");
			Constraint constraint2 = constraint.withOptions(null);
			assertThat(constraint2).isSameAs(constraint);

			constraint = Constraint.forNode("X").named("x").unique("x").withOptions("x");
			constraint2 = constraint.withOptions("x");
			assertThat(constraint2).isSameAs(constraint);
		}

		@Test
		void shouldModifyOptions() {

			Constraint constraint = Constraint.forNode("X").named("x").unique("x").withOptions("ox");
			assertThat(constraint.getOptionalOptions()).hasValue("ox");
		}

		@Test
		void shouldReturnSameOnUnchangedName() {

			Constraint constraint = Constraint.forNode("X").named("nx").unique("x");
			Constraint constraint2 = constraint.withName("nx");
			assertThat(constraint2).isSameAs(constraint);
		}

		@Test
		void shouldModifyName() {

			Constraint constraint = Constraint.forNode("X").named("x").unique("x").withName("a brand new name");
			assertThat(constraint.getName()).isEqualTo(Name.of("a brand new name"));
		}

	}

}
