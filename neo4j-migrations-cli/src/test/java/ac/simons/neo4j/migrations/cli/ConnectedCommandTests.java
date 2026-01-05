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
package ac.simons.neo4j.migrations.cli;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.ClientException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class ConnectedCommandTests {

	@Test
	void aWildExceptionIsNotACatalogException() {
		assertThat(ConnectedCommand.isCatalogException(new ClientException("foo"))).isFalse();
	}

	@Test
	void aWildIllegalArgumentIsNotACatalogException() {
		assertThat(ConnectedCommand.isCatalogException(new IllegalArgumentException("foo"))).isFalse();
	}

	@Test
	void aWildIllegalStateIsNotACatalogException() {
		assertThat(ConnectedCommand.isCatalogException(new IllegalArgumentException("foo"))).isFalse();
	}

	@Test
	void noStackTraceNoCatalogException() {
		var foo = new IllegalArgumentException("foo");
		foo.setStackTrace(new StackTraceElement[0]);
		assertThat(ConnectedCommand.isCatalogException(foo)).isFalse();
	}

	@Test
	void aCatalogExceptionIsACatalogException1() {
		var renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		var constraint = Constraint.forNode("F").named("f").key("f");
		var renderConfig = RenderConfig.create().forVersionAndEdition(Neo4jVersion.LATEST, Neo4jEdition.COMMUNITY);
		try {
			renderer.render(constraint, renderConfig);
			Assertions.fail("Exception not thrown as expected.");
		}
		catch (IllegalStateException ex) {
			assertThat(ConnectedCommand.isCatalogException(ex)).isTrue();
		}
	}

	@Test
	void aCatalogExceptionIsACatalogException2() {
		var renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		var constraint = Constraint.forNode("F").named("f").unique("a", "b");
		var renderConfig = RenderConfig.create().forVersionAndEdition(Neo4jVersion.V3_5, Neo4jEdition.COMMUNITY);
		try {
			renderer.render(constraint, renderConfig);
			Assertions.fail("Exception not thrown as expected.");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ConnectedCommand.isCatalogException(ex)).isTrue();
		}
	}

}
