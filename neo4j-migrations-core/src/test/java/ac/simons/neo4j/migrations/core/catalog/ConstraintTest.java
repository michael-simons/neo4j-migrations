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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.MapAccessorAndRecordImpl;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;

/**
 * @author Michael J. Simons
 */
class ConstraintTest {

	static Stream<Arguments> shouldParseUniqueNode() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ( book:Book ) ASSERT book.isbn IS UNIQUE"),
			Arguments.of("4.0", "a_name", "CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE"),
			Arguments.of("4.0", "stupid_stuff",
				"CONSTRAINT ON ( book:Book ) ASSERT (book.fünny things are fünny \uD83D\uDE31. Wow.) IS UNIQUE"),
			Arguments.of("4.1", "a_name", "CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseUniqueNode(String version, String name, String description) {

		Constraint constraint = Constraint.parse(
			new MapAccessorAndRecordImpl(makeMap(new SimpleEntry<>("name", name == null ? Values.NULL : Values.value(name)),
				new SimpleEntry<>("description", Values.value(description)))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.UNIQUE);
		assertThat(constraint.getTarget()).isEqualTo(TargetEntity.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("fünny things are fünny 😱. Wow.");
		} else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			} else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}

			assertThat(constraint.getProperties()).containsExactly("isbn");
		}
	}

	static Stream<Arguments> shouldParseSimpleNodePropertyExistenceConstraint() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)"),
			Arguments.of("4.0", "a_name", "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)"),
			Arguments.of("4.0", "stupid_stuff",
				"CONSTRAINT ON ( book:Book ) ASSERT exists(book.fünny things are fünny and why not, add more fun. Wow \uD83D\uDE31)"),
			Arguments.of("4.1", "a_name", "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseSimpleNodePropertyExistenceConstraint(String version, String name, String description) {

		Constraint constraint = Constraint.parse(
			new MapAccessorAndRecordImpl(makeMap(new SimpleEntry<>("name", name == null ? Values.NULL : Values.value(name)),
				new SimpleEntry<>("description", Values.value(description)))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.EXISTS);
		assertThat(constraint.getTarget()).isEqualTo(TargetEntity.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly(
				"fünny things are fünny and why not, add more fun. Wow 😱");
		} else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			} else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("isbn");
		}
	}

	static Stream<Arguments> shouldParseNodeKeyConstraint() {
		return Stream.of(
			Arguments.of("3.5", null,
				"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY"),
			Arguments.of("4.0", "a_name",
				"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY"),
			Arguments.of("4.0", "stupid_stuff",
				"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname, person.person.whatever, person.person.a,person.b) IS NODE KEY"),
			Arguments.of("4.1", "constraint_name1",
				"CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseNodeKeyConstraint(String version, String name, String description) {

		Constraint constraint = Constraint.parse(
			new MapAccessorAndRecordImpl(makeMap(new SimpleEntry<>("name", name == null ? Values.NULL : Values.value(name)),
				new SimpleEntry<>("description", Values.value(description)))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.KEY);
		assertThat(constraint.getTarget()).isEqualTo(TargetEntity.NODE);
		assertThat(constraint.getIdentifier()).isEqualTo("Person");
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("firstname", "surname", "person.whatever",
				"person.a,person.b");
		} else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			} else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("firstname", "surname");
		}
	}

	static Stream<Arguments> shouldParseSimpleRelPropertyExistenceConstraint() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"),
			Arguments.of("3.5", "stupid_stuff", "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.x,liked.y)"),
			Arguments.of("4.0", "constraint_name", "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"),
			Arguments.of("4.1", "constraint_name", "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseSimpleRelPropertyExistenceConstraint(String version, String name, String description) {

		Constraint constraint = Constraint.parse(
			new MapAccessorAndRecordImpl(makeMap(new SimpleEntry<>("name", name == null ? Values.NULL : Values.value(name)),
				new SimpleEntry<>("description", Values.value(description)))));
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.EXISTS);
		assertThat(constraint.getIdentifier()).isEqualTo("LIKED");
		assertThat(constraint.getTarget()).isEqualTo(TargetEntity.RELATIONSHIP);
		if ("stupid_stuff".equals(name)) {
			assertThat(constraint.getProperties()).containsExactly("x,liked.y");
		} else {
			if (name == null) {
				assertThat(constraint.hasName()).isFalse();
			} else {
				assertThat(constraint.getName()).isEqualTo(Name.of(name));
			}
			assertThat(constraint.getProperties()).containsExactly("day");
		}
	}

	@SafeVarargs
	static Map<String, Value> makeMap(SimpleEntry<String, Value>... entries) {
		Map<String, Value> result = new HashMap<>(entries.length);
		for (SimpleEntry<String, Value> entry : entries) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	static Stream<Arguments> showConstraints44() {
		String v = "4.4";

		return Stream.of(
			Arguments.of(
				v,
				Constraint.Type.UNIQUE,
				"Book",
				TargetEntity.NODE,
				Collections.singletonList("title"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("UNIQUENESS")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("title")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.KEY,
				"Person",
				TargetEntity.NODE,
				Arrays.asList("firstname", "surname"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("NODE_KEY")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Person"))),
					new SimpleEntry<>("properties", Values.value(Arrays.asList("firstname", "surname")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.UNIQUE,
				"Book",
				TargetEntity.NODE,
				Arrays.asList("a", "b"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("UNIQUENESS")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Arrays.asList("a", "b")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.EXISTS,
				"Book",
				TargetEntity.NODE,
				Collections.singletonList("isbn"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("NODE_PROPERTY_EXISTENCE")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("isbn")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.EXISTS,
				"LIKED",
				TargetEntity.RELATIONSHIP,
				Collections.singletonList("day"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("RELATIONSHIP_PROPERTY_EXISTENCE")),
					new SimpleEntry<>("entityType", Values.value("RELATIONSHIP")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("LIKED"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("day")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.UNIQUE,
				"Book",
				TargetEntity.NODE,
				Collections.singletonList("isbn"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("UNIQUENESS")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("isbn")))
				)
			)
		);
	}

	static Stream<Arguments> shouldDealWithShowConstraints() {

		return Stream.concat(Stream.of("4.2", "4.3").flatMap(v -> Stream.of(
			Arguments.of(
				v,
				Constraint.Type.KEY,
				"Person",
				TargetEntity.NODE,
				Arrays.asList("firstname", "surname"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("NODE_KEY")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Person"))),
					new SimpleEntry<>("properties", Values.value(Arrays.asList("firstname", "surname")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.EXISTS,
				"Book",
				TargetEntity.NODE,
				Collections.singletonList("isbn"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("NODE_PROPERTY_EXISTENCE")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("isbn")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.EXISTS,
				"LIKED",
				TargetEntity.RELATIONSHIP,
				Collections.singletonList("day"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("RELATIONSHIP_PROPERTY_EXISTENCE")),
					new SimpleEntry<>("entityType", Values.value("RELATIONSHIP")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("LIKED"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("day")))
				)
			),
			Arguments.of(
				v,
				Constraint.Type.UNIQUE,
				"Book",
				TargetEntity.NODE,
				Collections.singletonList("isbn"),
				makeMap(
					new SimpleEntry<>("name", Values.value("constraint_name")),
					new SimpleEntry<>("type", Values.value("UNIQUENESS")),
					new SimpleEntry<>("entityType", Values.value("NODE")),
					new SimpleEntry<>("labelsOrTypes", Values.value(Collections.singletonList("Book"))),
					new SimpleEntry<>("properties", Values.value(Collections.singletonList("isbn")))
				)
			)
		)), showConstraints44());
	}

	@ParameterizedTest
	@MethodSource
	void shouldDealWithShowConstraints(
		String version,
		Constraint.Type expectedType,
		String expectedIdentifier,
		TargetEntity expectedTarget,
		Collection<String> expectedProperties,
		Map<String, Value> content) {

		MapAccessor row = new MapAccessorAndRecordImpl(content);
		Constraint constraint = Constraint.parse(row);

		assertThat(constraint.getType()).isEqualTo(expectedType);
		assertThat(constraint.getIdentifier()).isEqualTo(expectedIdentifier);
		assertThat(constraint.getTarget()).isEqualTo(expectedTarget);
		assertThat(constraint.getProperties()).containsExactlyElementsOf(expectedProperties);
		assertThat(constraint.getName()).isEqualTo(Name.of("constraint_name"));
	}

	@Nested
	class Builder {

		@ParameterizedTest
		@EnumSource(Constraint.Type.class)
		void nodeConstraintBuilderShouldWork(Constraint.Type type) {
			Constraint constraint = null;

			switch (type) {
				case UNIQUE:
					constraint = Constraint.forNode("Book").named("foo").unique("bar");
					break;
				case EXISTS:
					constraint = Constraint.forNode("Book").named("foo").exists("bar");
					break;
				case KEY:
					constraint = Constraint.forNode("Book").named("foo").key("bar");
					break;
				default:
					Assertions.fail("Unsupported type: " + type);
			}

			assertThat(constraint.getIdentifier()).isEqualTo("Book");
			assertThat(constraint.getTarget()).isEqualTo(TargetEntity.NODE);
			assertThat(constraint.getProperties()).containsExactly("bar");
			assertThat(constraint.getType()).isEqualTo(type);
			assertThat(constraint.getName()).isEqualTo(Name.of("foo"));
		}
	}

	@Nested
	class Invalid {

		@Test
		void keyConstraintsShouldNotBeSupportedOnRelationships() {
			List<String> properties = Collections.singletonList("x");
			assertThatIllegalArgumentException().isThrownBy(
				() -> new Constraint(Constraint.Type.KEY, TargetEntity.RELATIONSHIP, "LIKES", properties)
			).withMessage("Key constraints are only supported for nodes, not for relationships.");
		}
	}

	@Nested
	class Equivalence {

		Constraint uniqueBookIdV1 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("id");

		@Test
		void shouldNotBeEquivalentToOtherThings() {

			assertThat(uniqueBookIdV1.isEquivalentTo(new AbstractCatalogItem<Constraint.Type>("book_id_unique", Constraint.Type.UNIQUE, TargetEntity.NODE, "Book", Collections.singletonList("id"), null) {
				@Override public boolean isEquivalentTo(CatalogItem<?> that) {
					return false;
				}
			})).isFalse();
		}

		@Test
		void shouldNotBeEquivalentToSame() {

			assertThat(uniqueBookIdV1.isEquivalentTo(uniqueBookIdV1)).isTrue();
		}

		@Test
		void sameTypeIsRequired() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forNode("Book")
					.named("book_id_unique")
					.exists("id")
			)).isFalse();
		}

		@Test
		void sameEntityIsRequired() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forRelationship("LIKES")
					.named("book_id_unique")
					.unique("id")
			)).isFalse();
		}

		@Test
		void sameIdentifierIsRequired() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forNode("SomethingElse")
					.named("book_id_unique")
					.unique("id")
			)).isFalse();
		}

		@Test
		void sameOptionsAreRequired() {

			Constraint other = new Constraint(null, Constraint.Type.UNIQUE, TargetEntity.NODE, "Book", Collections.singleton("id"), "foo");
			assertThat(uniqueBookIdV1.isEquivalentTo(other)).isFalse();
		}

		@Test
		void nullOptionsShouldBeSame() {

			Constraint other = new Constraint(null, Constraint.Type.UNIQUE, TargetEntity.NODE, "Book", Collections.singleton("id"), " ");
			assertThat(uniqueBookIdV1.isEquivalentTo(other)).isTrue();
		}

		@Test
		void samePropertiesAreRequired() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forNode("Book")
					.named("book_id_unique")
					.unique("ids")
			)).isFalse();
		}

		@Test
		void nameIsIrrelevant() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forNode("Book")
					.named("foo")
					.unique("id")
			)).isTrue();
		}

		@Test
		void allFieldsAndTheNameShouldWorkToo() {

			assertThat(uniqueBookIdV1.isEquivalentTo(
				Constraint
					.forNode("Book")
					.named("book_id_unique")
					.unique("id")
			)).isTrue();
		}
	}
}
