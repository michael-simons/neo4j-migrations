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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.Neo4jEdition;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class ConstraintRendererTest {

	static Stream<Arguments> multiplePropertiesAreOnlySupportedOn44AndHigher() {

		return Stream.concat(
			Stream.of(Arguments.of("3.5", true), Arguments.of("4.4", false), Arguments.of("5.0", false)),
			IntStream.range(0, 4).mapToObj(i -> Arguments.of("4." + i, true)));
	}

	@ParameterizedTest
	@MethodSource
	void multiplePropertiesAreOnlySupportedOn44AndHigher(String serverVersion, boolean shouldFail) {

		RenderContext renderContext = new RenderContext(serverVersion, Neo4jEdition.COMMUNITY, Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.UNIQUE, TargetEntity.NODE,
			"Book",
			Arrays.asList("a", "b"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		if (shouldFail) {
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> renderer.render(constraint, renderContext));
		} else {
			assertThat(renderer.render(constraint, renderContext)).isEqualTo(
				"CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE (n.a, n.b) IS UNIQUE");
		}
	}

	@ParameterizedTest
	@EnumSource(value = Constraint.Type.class, names = { "UNIQUE", "KEY" }, mode = EnumSource.Mode.EXCLUDE)
	void multiplePropertiesAreOnlySupportedWithUniqueConstraints(Constraint.Type type) {

		RenderContext renderContext = new RenderContext("4.4", Neo4jEdition.ENTERPRISE, Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", type, TargetEntity.NODE, "Book",
			Arrays.asList("a", "b"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(() -> renderer.render(constraint, renderContext));
	}

	static Stream<Arguments> shouldRenderSimpleUniqueConstraint() {

		return Stream.of(
				Arguments.of("3.5", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.0", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.1", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.1", Neo4jEdition.UNKNOWN, true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.2", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.2", Neo4jEdition.UNKNOWN, true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.3", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.3", Neo4jEdition.UNKNOWN, true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.4", Neo4jEdition.UNKNOWN, false, "CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE n.isbn IS UNIQUE"),
				Arguments.of("4.4", Neo4jEdition.UNKNOWN, true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Book) REQUIRE n.isbn IS UNIQUE")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleUniqueConstraint(String serverVersion, Neo4jEdition edition, boolean idempotent,
	                                        String expected) {

		RenderContext renderContext = new RenderContext(serverVersion, edition, Operator.CREATE, idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.UNIQUE, TargetEntity.NODE, "Book",
				Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "3.5", "4.0" })
	void shouldNotDoIdempotencyOnOldVersions(String version) {

		RenderContext renderContext = new RenderContext(version, Neo4jEdition.COMMUNITY, Operator.CREATE, true);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.UNIQUE, TargetEntity.NODE,
			"Book",
			Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(() -> renderer.render(constraint, renderContext))
			.withMessage("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.", version);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jEdition.class, names = "ENTERPRISE", mode = EnumSource.Mode.EXCLUDE)
	void nodePropertyExistenceConstraintShouldRequireEE(Neo4jEdition edition) {

		RenderContext renderContext = new RenderContext("3.5", edition, Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.EXISTS, TargetEntity.NODE, "Book",
				Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> renderer.render(constraint, renderContext))
				.withMessage("This constraint cannot be be used with %s edition.", edition);
	}

	static Stream<Arguments> shouldRenderSimpleNodePropertyExistenceConstraint() {

		return Stream.of(
				Arguments.of("3.5", false, "CREATE CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.0", false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.1", false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.1", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.2", false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.2", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.3", false, "CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS NOT NULL"),
				Arguments.of("4.3", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS NOT NULL"),
				Arguments.of("4.4", false, "CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE n.isbn IS NOT NULL"),
				Arguments.of("4.4", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Book) REQUIRE n.isbn IS NOT NULL")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleNodePropertyExistenceConstraint(String serverVersion, boolean idempotent, String expected) {

		RenderContext renderContext = new RenderContext(serverVersion, Neo4jEdition.ENTERPRISE, Operator.CREATE, idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.EXISTS, TargetEntity.NODE, "Book",
				Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRenderSimpleRelPropertyExistenceConstraint() {
		return Stream.of(
				Arguments.of("3.5", false, "CREATE CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.0", false, "CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.1", false, "CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.1", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.2", false, "CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.2", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.3", false, "CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT r.day IS NOT NULL"),
				Arguments.of("4.3", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT r.day IS NOT NULL"),
				Arguments.of("4.4", false, "CREATE CONSTRAINT constraint_name FOR ()-[r:LIKED]-() REQUIRE r.day IS NOT NULL"),
				Arguments.of("4.4", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR ()-[r:LIKED]-() REQUIRE r.day IS NOT NULL")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleRelPropertyExistenceConstraint(String serverVersion, boolean idempotent, String expected) {

		RenderContext renderContext = new RenderContext(serverVersion, Neo4jEdition.ENTERPRISE, Operator.CREATE, idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.EXISTS, TargetEntity.RELATIONSHIP, "LIKED",
				Collections.singleton("day"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}

	static Stream<Arguments> shouldRendereNodeKeyConstraint() {

		return Stream.of(
			Arguments.of("3.5", false, "CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.0", false, "CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.1", false, "CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.1", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.2", false, "CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.2", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.3", false, "CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.3", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.4", false, "CREATE CONSTRAINT constraint_name FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY"),
			Arguments.of("4.4", true, "CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRendereNodeKeyConstraint(String serverVersion, boolean idempotent, String expected) {

		RenderContext renderContext = new RenderContext(serverVersion, Neo4jEdition.ENTERPRISE, Operator.CREATE,
			idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.KEY, TargetEntity.NODE, "Person",
			Arrays.asList("firstname", "surname"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}
}