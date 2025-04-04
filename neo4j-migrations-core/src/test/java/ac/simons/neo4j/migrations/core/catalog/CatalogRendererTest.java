/*
 * Copyright 2020-2025 the original author or authors.
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
import ac.simons.neo4j.migrations.core.Neo4jVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Michael J. Simons
 */
class CatalogRendererTest {

	private static final Catalog CATALOG = Catalog.of(Arrays.asList(
		Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("id"),
		Constraint.forNode("Book")
			.named("book_isbn_exists")
			.exists("isbn"),
		Index.forNode("Person")
			.named("person_name")
			.onProperties("name")
	));

	@Test
	void shouldRenderCatalogAsXml() {

		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
		assertThat(renderer.render(CATALOG, RenderConfig.defaultConfig())).isEqualTo(
			""
			+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
			+ "    <catalog>\n"
			+ "        <indexes>\n"
			+ "            <index name=\"person_name\" type=\"property\">\n"
			+ "                <label>Person</label>\n"
			+ "                <properties>\n"
			+ "                    <property>name</property>\n"
			+ "                </properties>\n"
			+ "            </index>\n"
			+ "        </indexes>\n"
			+ "        <constraints>\n"
			+ "            <constraint name=\"book_id_unique\" type=\"unique\">\n"
			+ "                <label>Book</label>\n"
			+ "                <properties>\n"
			+ "                    <property>id</property>\n"
			+ "                </properties>\n"
			+ "            </constraint>\n"
			+ "            <constraint name=\"book_isbn_exists\" type=\"exists\">\n"
			+ "                <label>Book</label>\n"
			+ "                <properties>\n"
			+ "                    <property>isbn</property>\n"
			+ "                </properties>\n"
			+ "            </constraint>\n"
			+ "        </constraints>\n"
			+ "    </catalog>\n"
			+ "</migration>\n"
			+ "");
	}

	@Test
	void shouldAddReset() {

		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
		assertThat(renderer.render(Catalog.of(Collections.emptyList()), RenderConfig.defaultConfig().withAdditionalOptions(Collections.singletonList(
			new RenderConfig.XMLRenderingOptions() {
				@Override
				public boolean withReset() {
					return true;
				}
			})))).isEqualTo(
			""
			+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
			+ "    <catalog reset=\"true\">\n"
			+ "        <indexes/>\n"
			+ "        <constraints/>\n"
			+ "    </catalog>\n"
			+ "</migration>\n"
			+ "");
	}

	@Test
	void shouldAddApply() {

		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
		assertThat(renderer.render(Catalog.of(Collections.emptyList()), RenderConfig.defaultConfig().withAdditionalOptions(Collections.singletonList(
			new RenderConfig.XMLRenderingOptions() {
				@Override
				public boolean withApply() {
					return true;
				}
			})))).isEqualTo(
			""
			+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
			+ "    <catalog>\n"
			+ "        <indexes/>\n"
			+ "        <constraints/>\n"
			+ "    </catalog>\n"
			+ "    <apply/>\n"
			+ "</migration>\n"
			+ "");
	}

	@Test
	void shouldAddComments() {

		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
		assertThat(renderer.render(Catalog.of(Collections.emptyList()), RenderConfig.defaultConfig().withAdditionalOptions(Collections.singletonList(
			new RenderConfig.XMLRenderingOptions() {
				@Override
				public Optional<String> optionalHeader() {
					return Optional.of("I was there.");
				}
			})))).isEqualTo(
			""
			+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
			+ "    <!-- I was there. -->\n"
			+ "    <catalog>\n"
			+ "        <indexes/>\n"
			+ "        <constraints/>\n"
			+ "    </catalog>\n"
			+ "</migration>\n"
			+ "");
	}

	@Test
	void shouldNotRenderUnsupportedItems() {
		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
		Catalog c = Catalog.of(Collections.singletonList(new AbstractCatalogItem<>("Unknown", Index.Type.LOOKUP, TargetEntityType.NODE, "a", List.of(), null) {
		}));
		assertThat(renderer.render(c, RenderConfig.defaultConfig())).isEqualTo(
			""
			+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
			+ "    <catalog>\n"
			+ "        <indexes/>\n"
			+ "        <constraints/>\n"
			+ "    </catalog>\n"
			+ "</migration>\n"
			+ "");
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> shouldRenderCatalogAsCypher() {

		return Stream.of(
			Arguments.of(Neo4jVersion.V3_5, String.format(
				"CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE;%nCREATE CONSTRAINT ON (n:Book) ASSERT exists(n.isbn);%nCREATE INDEX ON :Person(name);%n")),
			Arguments.of(Neo4jVersion.V4_4, String.format(
				"CREATE CONSTRAINT book_id_unique FOR (n:Book) REQUIRE n.id IS UNIQUE;%nCREATE CONSTRAINT book_isbn_exists FOR (n:Book) REQUIRE n.isbn IS NOT NULL;%nCREATE INDEX person_name FOR (n:Person) ON (n.name);%n")
			));
	}

	@ParameterizedTest
	@MethodSource
	void shouldRenderCatalogAsCypher(Neo4jVersion version, String expected) {
		Renderer<Catalog> renderer = Renderer.get(Renderer.Format.CYPHER, Catalog.class);
		assertThat(renderer.render(CATALOG,
			RenderConfig.create().forVersionAndEdition(version, Neo4jEdition.ENTERPRISE))).isEqualTo(expected);
	}
}
