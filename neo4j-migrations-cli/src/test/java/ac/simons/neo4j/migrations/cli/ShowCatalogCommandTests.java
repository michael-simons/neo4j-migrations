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

import java.util.Collections;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
class ShowCatalogCommandTests {

	public static final Catalog CATALOG = Catalog
		.of(Collections.singletonList(Constraint.forNode("Book").named("book_id_unique").unique("id")));

	private static void setMode(ShowCatalogCommand cmd, ShowCatalogCommand.Mode mode) {
		ReflectionSupport
			.findFields(ShowCatalogCommand.class, f -> f.getName().equals("mode"), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, mode);
				}
				catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			});
	}

	private static void setFormatToCypher(ShowCatalogCommand cmd) {
		ReflectionSupport
			.findFields(ShowCatalogCommand.class, f -> f.getName().equals("format"), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, Renderer.Format.CYPHER);
				}
				catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			});
	}

	private static void setVersion(ShowCatalogCommand cmd, String version) {
		ReflectionSupport
			.findFields(ShowCatalogCommand.class, f -> f.getName().equals("version"), HierarchyTraversalMode.TOP_DOWN)
			.forEach(f -> {
				f.setAccessible(true);
				try {
					f.set(cmd, version);
				}
				catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			});
	}

	@Test
	void shouldUseCorrectCatalogs() throws Exception {

		String result = tapSystemOut(() -> {
			ShowCatalogCommand cmd = new ShowCatalogCommand();

			Migrations remoteAsXml = mock(Migrations.class);
			given(remoteAsXml.getDatabaseCatalog()).willReturn(CATALOG);
			setMode(cmd, ShowCatalogCommand.Mode.REMOTE);
			cmd.withMigrations(remoteAsXml);

			Migrations localAsCypher = mock(Migrations.class);
			given(localAsCypher.getLocalCatalog()).willReturn(CATALOG);
			setMode(cmd, ShowCatalogCommand.Mode.LOCAL);
			setFormatToCypher(cmd);
			cmd.withMigrations(localAsCypher);

			Migrations localAsCypherWithVersion = mock(Migrations.class);
			given(localAsCypherWithVersion.getLocalCatalog()).willReturn(CATALOG);
			setMode(cmd, ShowCatalogCommand.Mode.LOCAL);
			setVersion(cmd, "3.5.23");
			setFormatToCypher(cmd);
			cmd.withMigrations(localAsCypherWithVersion);

			verify(remoteAsXml).getDatabaseCatalog();
			verify(localAsCypher).getLocalCatalog();
			verify(localAsCypherWithVersion).getLocalCatalog();
			verifyNoMoreInteractions(remoteAsXml, localAsCypher, localAsCypherWithVersion);
			System.out.flush();
		});

		String nl = System.lineSeparator();
		assertThat(result).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + nl
				+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">" + nl + "    <catalog>" + nl
				+ "        <indexes/>" + nl + "        <constraints>" + nl
				+ "            <constraint name=\"book_id_unique\" type=\"unique\">" + nl
				+ "                <label>Book</label>" + nl + "                <properties>" + nl
				+ "                    <property>id</property>" + nl + "                </properties>" + nl
				+ "            </constraint>" + nl + "        </constraints>" + nl + "    </catalog>" + nl
				+ "</migration>" + nl
				+ "CREATE CONSTRAINT book_id_unique IF NOT EXISTS FOR (n:Book) REQUIRE n.id IS UNIQUE;" + nl
				+ "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE;" + nl);
	}

}
