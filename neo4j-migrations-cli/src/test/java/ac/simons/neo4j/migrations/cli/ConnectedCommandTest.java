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
package ac.simons.neo4j.migrations.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.ClientException;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;

/**
 * @author Michael J. Simons
 */
class ConnectedCommandTest {

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
	void aCatalogExceptionIsACatalogException1() {
		var renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		try {
			renderer.render(Constraint.forNode("F").named("f").key("f"), RenderConfig.create().forVersionAndEdition(Neo4jVersion.LATEST, Neo4jEdition.COMMUNITY));
			Assertions.fail("Exception not thrown as expected.");
		} catch (IllegalStateException e) {
			assertThat(ConnectedCommand.isCatalogException(e)).isTrue();
		}
	}

	@Test
	void aCatalogExceptionIsACatalogException2() {
		var renderer = Renderer.get(Renderer.Format.CYPHER, Constraint.class);
		try {
			renderer.render(Constraint.forNode("F").named("f").unique("a", "b"), RenderConfig.create().forVersionAndEdition(Neo4jVersion.V3_5, Neo4jEdition.COMMUNITY));
			Assertions.fail("Exception not thrown as expected.");
		} catch (IllegalArgumentException e) {
			assertThat(ConnectedCommand.isCatalogException(e)).isTrue();
		}
	}
}
