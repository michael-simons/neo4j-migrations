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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ac.simons.neo4j.migrations.core.CatalogBasedMigration.DropOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Operation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.OperationContext;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.net.URL;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.neo4j.driver.QueryRunner;

/**
 * @author Michael J. Simons
 */
class CatalogBasedMigrationTest {

	@ParameterizedTest
	@ValueSource(strings = { "01.xml", "02.xml", "03.xml" })
	void checksumShouldBeCorrect(String in) {
		URL url = CatalogBasedMigration.class.getResource("/xml/identical-migrations/V01__" + in);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.getChecksum()).hasValue("620177586");
		assertThat(schemaBasedMigration.getCatalog().getItems()).hasSize(2);
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Drops {

		final ArgumentCaptor<String> configArgumentCaptor = ArgumentCaptor.forClass(String.class);

		Constraint uniqueBookIdV1 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("id");

		VersionedCatalog catalog;

		@BeforeEach
		void initMocks() {

			catalog = new DefaultCatalog();
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("1"),
				() -> Collections.singletonList(uniqueBookIdV1));
		}

		Stream<Arguments> simpleDropShouldWork() {

			return Stream.of(
				Arguments.of(Neo4jVersion.V3_5, "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Neo4jVersion.V4_4, "DROP CONSTRAINT book_id_unique")
			);
		}

		@ParameterizedTest
		@MethodSource
		void simpleDropShouldWork(Neo4jVersion version, String expected) {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), false)
				.with(MigrationVersion.withValue("1"));
			QueryRunner runner = mock(QueryRunner.class);
			OperationContext context = new OperationContext(version, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);
			verify(runner).run(configArgumentCaptor.capture());
			String query = configArgumentCaptor.getValue();

			assertThat(query).isEqualTo(expected);
		}
	}
}
