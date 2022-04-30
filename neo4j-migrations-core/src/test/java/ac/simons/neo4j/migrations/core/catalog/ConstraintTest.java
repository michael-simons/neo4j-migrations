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

import ac.simons.neo4j.migrations.core.Neo4jEdition;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Michael J. Simons
 */
class ConstraintTest {

	static Stream<Arguments> shouldParseUniqueNode() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ( book:Book ) ASSERT book.isbn IS UNIQUE")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseUniqueNode(String version, String name, String description) {

		ConstraintDescription constraintDescription = new ConstraintDescription(version, Neo4jEdition.UNKNOWN, name,
			description);
		Constraint constraint = Constraint.of(constraintDescription);
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.UNIQUE);
		if (name == null) {
			assertThat(constraint.getName().isBlank()).isTrue();
		} else {
			assertThat(constraint.getName()).isEqualTo(Name.of(name));
		}
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		assertThat(constraint.getProperties()).containsExactly("isbn");
	}

	static Stream<Arguments> shouldParseSimpleNodePropertyExistenceConstraint() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ( book:Book ) ASSERT exists(book.isbn)" )
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseSimpleNodePropertyExistenceConstraint(String version, String name, String description) {

		ConstraintDescription constraintDescription = new ConstraintDescription(version, Neo4jEdition.ENTERPRISE, name,
			description);
		Constraint constraint = Constraint.of(constraintDescription);
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.EXISTS);
		if (name == null) {
			assertThat(constraint.getName().isBlank()).isTrue();
		} else {
			assertThat(constraint.getName()).isEqualTo(Name.of(name));
		}
		assertThat(constraint.getIdentifier()).isEqualTo("Book");
		assertThat(constraint.getProperties()).containsExactly("isbn");
	}

	static Stream<Arguments> shouldParseNodeKeyConstraint() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ( person:Person ) ASSERT (person.firstname, person.surname) IS NODE KEY" )
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseNodeKeyConstraint(String version, String name, String description) {

		ConstraintDescription constraintDescription = new ConstraintDescription(version, Neo4jEdition.ENTERPRISE, name,
			description);
		Constraint constraint = Constraint.of(constraintDescription);
		assertThat(constraint.getType()).isEqualTo(Constraint.Type.KEY);
		if (name == null) {
			assertThat(constraint.getName().isBlank()).isTrue();
		} else {
			assertThat(constraint.getName()).isEqualTo(Name.of(name));
		}
		assertThat(constraint.getIdentifier()).isEqualTo("Person");
		assertThat(constraint.getProperties()).containsExactly("firstname", "surname");
	}

	static Stream<Arguments> shouldParseSimpleRelPropertyExistenceConstraint() {
		return Stream.of(
			Arguments.of("3.5", null, "CONSTRAINT ON ()-[ liked:LIKED ]-() ASSERT exists(liked.day)"   )
		);
	}
}
