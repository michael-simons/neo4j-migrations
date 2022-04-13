package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.Neo4jEdition;

import java.util.Collections;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
		Assertions.assertThat(renderer.render(constraint, renderContext)).isEqualTo(expected);
	}
}