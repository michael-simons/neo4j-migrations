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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	@Test
	void shouldFindElementsOfLeftNotInRightAndVsVersa() {

		List<CatalogItem<?>> left = Arrays.asList(uniqueBookIdV1, uniqueBookIdV2a);
		List<CatalogItem<?>> right = Arrays.asList(uniqueBookIdV1, likeAt);
		CatalogDiff diff = CatalogDiff.between(Catalog.of(left), Catalog.of(right));
		assertThat(diff.identical()).isFalse();
		assertThat(diff.equivalent()).isFalse();
		assertThat(diff.getItemsOnlyInLeft()).containsExactly(uniqueBookIdV2a);
		assertThat(diff.getItemsOnlyInRight()).containsExactly(likeAt);
	}

	@Test
	void shouldFindEquivalent() {

		List<CatalogItem<?>> left = Arrays.asList(uniqueBookIdV2a, likeAt);
		List<CatalogItem<?>> right = Arrays.asList(uniqueBookIdV2b, likeAt);
		CatalogDiff diff = CatalogDiff.between(Catalog.of(left), Catalog.of(right));
		assertThat(diff.identical()).isFalse();
		assertThat(diff.equivalent()).isTrue();
		assertThat(diff.getItemsOnlyInLeft()).containsExactly(uniqueBookIdV2a);
		assertThat(diff.getItemsOnlyInRight()).containsExactly(uniqueBookIdV2b);
		assertThat(diff.getEquivalentItems()).containsExactlyInAnyOrder(uniqueBookIdV2a, uniqueBookIdV2b);
	}
}
