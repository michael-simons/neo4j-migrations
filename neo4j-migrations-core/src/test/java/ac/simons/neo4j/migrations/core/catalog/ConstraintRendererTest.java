package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.MigrationsException;
import ac.simons.neo4j.migrations.core.Neo4jEdition;

import java.util.Collections;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class ConstraintRendererTest {

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
		Constraint constraint = new Constraint("constraint_name", Constraint.Kind.UNIQUE, Constraint.Target.NODE, "Book",
				Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}

	@ParameterizedTest
	@EnumSource(value = Neo4jEdition.class, names = "ENTERPRISE", mode = EnumSource.Mode.EXCLUDE)
	void nodePropertyExistenceConstraintShouldRequireEE(Neo4jEdition edition) {

		RenderContext renderContext = new RenderContext("3.5", edition, Operator.CREATE, false);
		Constraint constraint = new Constraint("constraint_name", Constraint.Kind.EXISTS, Constraint.Target.NODE, "Book",
				Collections.singleton("isbn"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		Assertions.assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> renderer.render(constraint, renderContext));
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
		Constraint constraint = new Constraint("constraint_name", Constraint.Kind.EXISTS, Constraint.Target.NODE, "Book",
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
		Constraint constraint = new Constraint("constraint_name", Constraint.Kind.EXISTS, Constraint.Target.RELATIONSHIP, "LIKED",
				Collections.singleton("day"));

		Renderer<Constraint> renderer = new ConstraintRenderer();
		assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}
}