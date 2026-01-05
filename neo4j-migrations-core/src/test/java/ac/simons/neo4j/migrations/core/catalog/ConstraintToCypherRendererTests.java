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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class ConstraintToCypherRendererTests {

	@SuppressWarnings("unused")
	static Stream<Arguments> multiplePropertiesAreOnlySupportedOn44AndHigher() {

		return Stream.concat(
				Stream.of(Arguments.of("3.5", true), Arguments.of("4.4", false), Arguments.of("5.0", false)),
				IntStream.range(0, 4).mapToObj(i -> Arguments.of("4." + i, true)));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderSimpleUniqueConstraint() {

		return Stream.of(
				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE n.isbn IS UNIQUE"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Book) REQUIRE n.isbn IS UNIQUE"),

				Arguments.of("3.5", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.0", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", true, Neo4jEdition.UNDEFINED, Operator.DROP, true,
						"DROP CONSTRAINT constraint_name IF EXISTS"),

				Arguments.of("4.0", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.1", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.2", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.3", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"),
				Arguments.of("4.4", false, Neo4jEdition.UNDEFINED, Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderSimpleNodePropertyExistenceConstraint() {

		return Stream.of(
				Arguments.of("3.5", true, Operator.CREATE, false,
						"CREATE CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.0", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.1", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.1", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.2", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.2", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.3", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Book) ASSERT n.isbn IS NOT NULL"),
				Arguments.of("4.3", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS NOT NULL"),
				Arguments.of("4.4", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE n.isbn IS NOT NULL"),
				Arguments.of("4.4", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Book) REQUIRE n.isbn IS NOT NULL"),

				Arguments.of("3.5", true, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.0", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),

				Arguments.of("4.0", false, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.1", false, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.2", false, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.3", false, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"),
				Arguments.of("4.4", false, Operator.DROP, false, "DROP CONSTRAINT ON (n:Book) ASSERT exists(n.isbn)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderSimpleRelPropertyExistenceConstraint() {
		return Stream.of(
				Arguments.of("3.5", true, Operator.CREATE, false,
						"CREATE CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.0", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.1", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.1", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.2", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.2", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.3", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON ()-[r:LIKED]-() ASSERT r.day IS NOT NULL"),
				Arguments.of("4.3", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT r.day IS NOT NULL"),
				Arguments.of("4.4", true, Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR ()-[r:LIKED]-() REQUIRE r.day IS NOT NULL"),
				Arguments.of("4.4", true, Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR ()-[r:LIKED]-() REQUIRE r.day IS NOT NULL"),

				Arguments.of("3.5", true, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.0", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", true, Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", true, Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),

				Arguments.of("4.0", false, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.1", false, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.2", false, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.3", false, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"),
				Arguments.of("4.4", false, Operator.DROP, false,
						"DROP CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day)"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRendereNodeKeyConstraint() {

		return Stream.of(
				Arguments.of("3.5", Operator.CREATE, false,
						"CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.0", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY"),

				Arguments.of("3.5", Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.0", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRendereSingleNodeKeyConstraint() {

		return Stream.of(
				Arguments.of("3.5", Operator.CREATE, false,
						"CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.0", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR (n:Person) REQUIRE n.firstname IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Person) REQUIRE n.firstname IS NODE KEY"),

				Arguments.of("3.5", Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Person) ASSERT (n.firstname) IS NODE KEY"),
				Arguments.of("4.0", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"));
	}

	static Stream<Arguments> shouldRenderPropertyTypeConstraints() {
		var constraintOnNode = Constraint.forNode("Movie").named("movie_title").type("title", PropertyType.STRING);
		var constraintOnRel = Constraint.forRelationship("HAD_ROLE")
			.named("whatever")
			.type("xyz", PropertyType.LOCAL_DATETIME);
		return Stream.of(
				Arguments.of(Operator.CREATE, false, constraintOnNode,
						"CREATE CONSTRAINT movie_title FOR (n:Movie) REQUIRE n.title IS :: STRING"),
				Arguments.of(Operator.CREATE, true, constraintOnNode,
						"CREATE CONSTRAINT movie_title IF NOT EXISTS FOR (n:Movie) REQUIRE n.title IS :: STRING"),
				Arguments.of(Operator.DROP, false, constraintOnNode, "DROP CONSTRAINT movie_title"),
				Arguments.of(Operator.DROP, true, constraintOnNode, "DROP CONSTRAINT movie_title IF EXISTS"),
				Arguments.of(Operator.CREATE, false, constraintOnRel,
						"CREATE CONSTRAINT whatever FOR ()-[r:HAD_ROLE]-() REQUIRE r.xyz IS :: LOCAL DATETIME"),
				Arguments.of(Operator.CREATE, true, constraintOnRel,
						"CREATE CONSTRAINT whatever IF NOT EXISTS FOR ()-[r:HAD_ROLE]-() REQUIRE r.xyz IS :: LOCAL DATETIME"),
				Arguments.of(Operator.DROP, false, constraintOnRel, "DROP CONSTRAINT whatever"),
				Arguments.of(Operator.DROP, true, constraintOnRel, "DROP CONSTRAINT whatever IF EXISTS"));
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> ignoreNameShouldWork() {

		return Stream.of(
				Arguments.of("3.5", Operator.CREATE, false,
						"CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.0", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.1", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.2", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.3", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, false,
						"CREATE CONSTRAINT constraint_name FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.4", Operator.CREATE, true,
						"CREATE CONSTRAINT constraint_name IF NOT EXISTS FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY"),

				Arguments.of("3.5", Operator.DROP, false,
						"DROP CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY"),
				Arguments.of("4.0", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.1", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.2", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.2", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.3", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.3", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"),
				Arguments.of("4.4", Operator.DROP, false, "DROP CONSTRAINT constraint_name"),
				Arguments.of("4.4", Operator.DROP, true, "DROP CONSTRAINT constraint_name IF EXISTS"));
	}

	@Test
	void shouldEscapeNames() {

		RenderConfig create = RenderConfig.create().forVersionAndEdition("4.4", "ENTERPRISE");
		RenderConfig drop = RenderConfig.drop().forVersionAndEdition("4.4", "ENTERPRISE");

		Constraint constraint = Constraint.forNode("Book").named("das ist ein test").unique("isbn");

		String cypher = Renderer.get(Renderer.Format.CYPHER, Constraint.class).render(constraint, create);
		assertThat(cypher).isEqualTo("CREATE CONSTRAINT `das ist ein test` FOR (n:Book) REQUIRE n.isbn IS UNIQUE");

		cypher = Renderer.get(Renderer.Format.CYPHER, Constraint.class).render(constraint, drop);
		assertThat(cypher).isEqualTo("DROP CONSTRAINT `das ist ein test`");
	}

	@ParameterizedTest
	@MethodSource
	void multiplePropertiesAreOnlySupportedOn44AndHigher(String serverVersion, boolean shouldFail) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), Neo4jEdition.COMMUNITY,
				Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
				Arrays.asList("a", "b"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		if (shouldFail) {
			assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint, renderConfig));
		}
		else {
			assertThat(renderer.render(constraint, renderConfig))
				.isEqualTo("CREATE CONSTRAINT constraint_name FOR (n:Book) REQUIRE (n.a, n.b) IS UNIQUE");
		}
	}

	@ParameterizedTest
	@EnumSource(value = Constraint.Type.class, names = { "UNIQUE", "KEY", "PROPERTY_TYPE" },
			mode = EnumSource.Mode.EXCLUDE)
	void multiplePropertiesAreOnlySupportedWithUniqueConstraints(Constraint.Type type) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, Operator.CREATE,
				false);
		Constraint constraint = new Constraint("constraint_name", type, TargetEntityType.NODE, "Book",
				List.of("a", "b"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint, renderConfig));
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleUniqueConstraint(String serverVersion, boolean named, Neo4jEdition edition,
			Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), edition, operator, idempotent);
		Constraint constraint = new Constraint(named ? "constraint_name" : null, Constraint.Type.UNIQUE,
				TargetEntityType.NODE, "Book", Collections.singleton("isbn"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "3.5", "4.0" })
	void shouldNotDoIdempotencyOnOldVersions(String version) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(version), Neo4jEdition.COMMUNITY, Operator.CREATE,
				true);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
				Collections.singleton("isbn"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint, renderConfig))
			.withMessage("The given constraint cannot be rendered in an idempotent fashion on Neo4j %s.", version);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jEdition.class, names = "ENTERPRISE", mode = EnumSource.Mode.EXCLUDE)
	void nodePropertyExistenceConstraintShouldRequireEE(Neo4jEdition edition) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.V3_5, edition, Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.EXISTS, TargetEntityType.NODE, "Book",
				Collections.singleton("isbn"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> renderer.render(constraint, renderConfig))
			.withMessage("This constraint cannot be created with %s edition.", edition);
	}

	@Test
	void shouldOptionallyRenderOptions() {

		RenderConfig renderConfig = RenderConfig.create().forVersionAndEdition("4.4", "ENTERPRISE");
		Constraint constraint = new Constraint("unique_isbn", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
				Collections.singleton("isbn"),
				"{`indexConfig`: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, `indexProvider`: \"native-btree-1.0\"}",
				null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig))
			.isEqualTo("CREATE CONSTRAINT unique_isbn FOR (n:Book) REQUIRE n.isbn IS UNIQUE");
		assertThat(renderer.render(constraint,
				renderConfig.withAdditionalOptions(Collections.singletonList(new RenderConfig.CypherRenderingOptions() {
					@Override
					public boolean includingOptions() {
						return true;
					}
				}))))
			.isEqualTo(
					"CREATE CONSTRAINT unique_isbn FOR (n:Book) REQUIRE n.isbn IS UNIQUE OPTIONS {`indexConfig`: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, `indexProvider`: \"native-btree-1.0\"}");
	}

	@Test // GH-1182
	void indexOptionsShouldBeRendered() {

		var renderConfig = RenderConfig.create().forVersionAndEdition("4.4", "ENTERPRISE");
		var index = Index.forNode("MyNode")
			.named("myFulltextIndex")
			.onProperties("myFulltextSearchProperty")
			.withType(Index.Type.FULLTEXT)
			.withOptions(" indexConfig: +{ `fulltext.analyzer`:\"whitespace\" }");

		var renderer = Renderer.get(Renderer.Format.CYPHER, Index.class);
		assertThat(renderer.render(index,
				renderConfig.withAdditionalOptions(List.of(new RenderConfig.CypherRenderingOptions() {
					@Override
					public boolean includingOptions() {
						return true;
					}
				}))))
			.isEqualTo(
					"CREATE FULLTEXT INDEX myFulltextIndex FOR (n:MyNode) ON EACH [n.`myFulltextSearchProperty`] OPTIONS {indexConfig: +{ `fulltext.analyzer`:\"whitespace\" }}");
	}

	@ParameterizedTest
	@CsvSource({ "CREATE, false", "DROP, true" })
	void idempotencyShouldRequireName(Operator operator, boolean fails) {

		Constraint constraint = new Constraint(Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book",
				Collections.singleton("isbn"), null);
		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of("4.4.4"), Neo4jEdition.COMMUNITY, operator, true);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		if (fails) {
			assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint, renderConfig))
				.withMessage("The constraint can only be rendered in the given context when having a name.");
		}
		else {
			assertThat(renderer.render(constraint, renderConfig))
				.isEqualTo("CREATE CONSTRAINT IF NOT EXISTS FOR (n:Book) REQUIRE n.isbn IS UNIQUE");
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleNodePropertyExistenceConstraint(String serverVersion, boolean named, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), Neo4jEdition.ENTERPRISE, operator,
				idempotent);
		Constraint constraint = new Constraint(named ? "constraint_name" : null, Constraint.Type.EXISTS,
				TargetEntityType.NODE, "Book", Collections.singleton("isbn"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderSimpleRelPropertyExistenceConstraint(String serverVersion, boolean named, Operator operator,
			boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), Neo4jEdition.ENTERPRISE, operator,
				idempotent);
		Constraint constraint = new Constraint(named ? "constraint_name" : null, Constraint.Type.EXISTS,
				TargetEntityType.RELATIONSHIP, "LIKED", Collections.singleton("day"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRendereNodeKeyConstraint(String serverVersion, Operator operator, boolean idempotent, String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), Neo4jEdition.ENTERPRISE, operator,
				idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.KEY, TargetEntityType.NODE, "Person",
				Arrays.asList("firstname", "surname"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource
	void shouldRendereSingleNodeKeyConstraint(String serverVersion, Operator operator, boolean idempotent,
			String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.of(serverVersion), Neo4jEdition.ENTERPRISE, operator,
				idempotent);
		Constraint constraint = new Constraint("constraint_name", Constraint.Type.KEY, TargetEntityType.NODE, "Person",
				List.of("firstname"), null);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@ParameterizedTest // GH-1011
	@MethodSource
	void shouldRenderPropertyTypeConstraints(Operator operator, boolean idempotent, Constraint constraint,
			String expected) {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE, operator,
				idempotent);

		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, renderConfig)).isEqualTo(expected);
	}

	@Test
	void shouldNotRenderPropertyTypeConstraintsWithoutAName() {

		RenderConfig renderConfig = new RenderConfig(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE, Operator.DROP,
				false);
		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);

		var constraint1 = new Constraint(null, Constraint.Type.PROPERTY_TYPE, TargetEntityType.NODE, "Movie",
				List.of("whatever"), PropertyType.DURATION);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint1, renderConfig))
			.withMessage("Property type constraints can only be dropped via name.");

		var constraint2 = Constraint.forNode("Movie").named("movie_title").type("title", PropertyType.STRING);
		assertThatIllegalArgumentException().isThrownBy(() -> renderer.render(constraint2, renderConfig.ignoreName()))
			.withMessage("Property type constraints can only be dropped via name.");
	}

	@ParameterizedTest
	@EnumSource(Neo4jVersion.class)
	void ignoreNameShouldWork(Neo4jVersion version) {

		Constraint constraint = Constraint.forNode("__Neo4jMigrationsLock")
			.named("__Neo4jMigrationsLock__has_unique_id")
			.unique("id");

		RenderConfig dropConfig = RenderConfig.drop()
			.forVersionAndEdition(version, Neo4jEdition.ENTERPRISE)
			.ignoreName();
		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		assertThat(renderer.render(constraint, dropConfig))
			.isEqualTo("DROP CONSTRAINT ON (n:__Neo4jMigrationsLock) ASSERT n.id IS UNIQUE");
	}

	@Test
	void shouldEscapeNonValidThings() {

		RenderConfig config = RenderConfig.create().forVersionAndEdition(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE);
		Renderer<Constraint> renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);

		Constraint constraint = Constraint.forNode("Das ist ein Buch")
			.named("book_id_unique")
			.unique("person.a,person.b 😱");
		assertThat(renderer.render(constraint, config)).isEqualTo(
				"CREATE CONSTRAINT book_id_unique FOR (n:`Das ist ein Buch`) REQUIRE n.`person.a,person.b 😱` IS UNIQUE");

		Constraint c2onstraint = Constraint.forRelationship("DAS IST KEIN BUCH")
			.named("book_id_unique")
			.exists("person.a,person.b 😱");
		assertThat(renderer.render(c2onstraint, config)).isEqualTo(
				"CREATE CONSTRAINT book_id_unique FOR ()-[r:`DAS IST KEIN BUCH`]-() REQUIRE r.`person.a,person.b 😱` IS NOT NULL");
	}

}
