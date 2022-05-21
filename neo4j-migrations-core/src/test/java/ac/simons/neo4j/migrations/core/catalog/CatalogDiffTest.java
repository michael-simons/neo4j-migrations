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

import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class CatalogDiffTest {

	final Constraint uniqueBookIdV1 = Constraint
		.forNode("Book")
		.named("book_id_unique")
		.unique("id");

	final Constraint uniqueBookIdV2 = Constraint
		.forNode("Book")
		.named("book_id_unique")
		.unique("isbn");

	@Test
	void emptyCatalogsShouldBeIdentical() {

		CatalogDiff diff = CatalogDiff.between(Catalog.empty(), Catalog.empty());
		assertThat(diff.identical()).isTrue();
		assertThat(diff.getItemsOnlyInLeft()).isEmpty();
		assertThat(diff.getItemsOnlyInRight()).isEmpty();
	}

	@Test
	void emptyCatalogsShouldBeEquivalent() {

		CatalogDiff diff = CatalogDiff.between(Catalog.empty(), Catalog.empty());
		assertThat(diff.equivalent()).isTrue();
		assertThat(diff.getItemsOnlyInLeft()).isEmpty();
		assertThat(diff.getItemsOnlyInRight()).isEmpty();
	}

	@Nested
	class EmptyCatalogShouldNeverBeIdenticalOrEquivalentToANonEmpty {

		@Test
		void leftRight() {

			CatalogDiff diff = CatalogDiff.between(Catalog.empty(), Catalog.of(Collections.singleton(uniqueBookIdV1)));
			assertThat(diff.identical()).isFalse();
			assertThat(diff.equivalent()).isFalse();
			assertThat(diff.getItemsOnlyInLeft()).isEmpty();
			assertThat(diff.getItemsOnlyInRight()).containsExactly(uniqueBookIdV1);
		}

		@Test
		void rightLeft() {

			CatalogDiff diff = CatalogDiff.between(Catalog.of(Collections.singleton(uniqueBookIdV1)), Catalog.empty());
			assertThat(diff.identical()).isFalse();
			assertThat(diff.equivalent()).isFalse();
			assertThat(diff.getItemsOnlyInLeft()).containsExactly(uniqueBookIdV1);
			assertThat(diff.getItemsOnlyInRight()).isEmpty();
		}
	}
}
