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

import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class CatalogResultTest {

	final Constraint uniqueBookIdV1 = Constraint
		.forNode("Book")
		.named("book_id_unique")
		.unique("id");

	final Constraint uniqueBookIdV2a = Constraint
		.forNode("Book")
		.named("book_id_unique")
		.unique("isbn");

	final Constraint uniqueBookIdV2b = Constraint
		.forNode("Book")
		.named("eindeutige_buch_id")
		.unique("isbn");

	final Constraint likeAt = Constraint
		.forRelationship("LIKED")
		.named("book_id_unique")
		.exists("at");

	@Test
	void localCatalogResultShouldPrettyPrint() {

		CatalogResult result = new LocalCatalogResult(Collections::emptyList);
		assertThat(result.prettyPrint()).isEqualTo("Local catalog with 0 item(s).");
	}

	@Nested
	class RemoteCatalogResultPrettyPrinting {

		@Test
		void nodatabase() {
			CatalogResult result = new RemoteCatalogResult(Optional.empty(), Collections::emptyList,
				Collections::emptyList);
			assertThat(result.prettyPrint()).isEqualTo(
				"Catalog for the default database with 0 item(s) is identical to the local catalog.");
		}

		@Test
		void withdatabase() {
			CatalogResult result = new RemoteCatalogResult(Optional.of("foo"), Collections::emptyList,
				Collections::emptyList);
			assertThat(result.prettyPrint()).isEqualTo(
				"Catalog for the database `foo` with 0 item(s) is identical to the local catalog.");
		}

		@Test
		void equivalent() {
			CatalogResult result = new RemoteCatalogResult(Optional.of("foo"),
				() -> Collections.singletonList(uniqueBookIdV2a),
				() -> Collections.singletonList(uniqueBookIdV2b));

			assertThat(result.prettyPrint()).isEqualTo(
				"Catalog for the database `foo` with 1 item(s) is equivalent to the local catalog.");
		}

		@Test
		void different() {
			CatalogResult result = new RemoteCatalogResult(Optional.of("foo"),
				() -> Arrays.asList(uniqueBookIdV1, uniqueBookIdV2a),
				() -> Collections.singletonList(likeAt));

			assertThat(result.prettyPrint()).isEqualTo(
				"Catalog for the database `foo` with 2 item(s) contains 2 item(s) not in the local catalog and misses 1 item(s) of the local catalog.");
		}
	}
}
