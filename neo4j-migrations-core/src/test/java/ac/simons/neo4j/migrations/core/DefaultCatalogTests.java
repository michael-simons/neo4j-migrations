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
package ac.simons.neo4j.migrations.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class DefaultCatalogTests {

	Constraint c1v1;

	Constraint c1v2;

	Constraint c2v1;

	Catalog catalog1;

	Catalog catalog2;

	@BeforeEach
	void initMocks() {

		this.c1v1 = Mockito.mock(Constraint.class);
		Name name1 = Name.of("cv1");
		given(this.c1v1.getName()).willReturn(name1);
		given(this.c1v1.getProperties()).willReturn(new HashSet<>(Arrays.asList("a", "b")));

		this.c1v2 = mock(Constraint.class);
		given(this.c1v2.getName()).willReturn(name1);
		given(this.c1v2.getProperties()).willReturn(new HashSet<>(Arrays.asList("a", "b", "c")));

		this.c2v1 = mock(Constraint.class);
		Name name2 = Name.of("cv2");
		given(this.c2v1.getName()).willReturn(name2);
		given(this.c2v1.getProperties()).willReturn(Collections.singleton("d"));

		this.catalog1 = () -> Collections.singletonList(this.c1v1);
		this.catalog2 = () -> Arrays.asList(this.c1v2, this.c2v1);
	}

	@Test
	void getAllItemsShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);

		assertThat(catalog.getItems()).map(CatalogItem::getName).map(Name::getValue).containsExactly("cv1", "cv2");
	}

	@Test
	void resetShouldPreventedAtUsedVersion() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		MigrationVersion v2 = MigrationVersion.withValue("2");
		catalog.addAll(v2, this.catalog2, false);

		assertThatIllegalArgumentException().isThrownBy(() -> catalog.addAll(v2, Collections::emptyList, true))
			.withMessage("Version 2 has already been used in this catalog.");
	}

	@Test
	void resetShouldPreventDoubleReset() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);
		MigrationVersion v3 = MigrationVersion.withValue("3");
		catalog.addAll(v3, Collections::emptyList, true);

		assertThatIllegalArgumentException().isThrownBy(() -> catalog.addAll(v3, Collections::emptyList, true))
			.withMessage("Catalog has been already reset at version 3");
	}

	@Test
	void resetShouldWork() {

		MigrationVersion v1 = MigrationVersion.withValue("1");
		MigrationVersion v2 = MigrationVersion.withValue("2");
		MigrationVersion v3 = MigrationVersion.withValue("3");
		MigrationVersion v4 = MigrationVersion.withValue("4");

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(v1, this.catalog1, false);
		catalog.addAll(v2, this.catalog2, false);

		catalog.addAll(v3, Collections::emptyList, true);
		Constraint c = Constraint.forNode("C").named("c").exists("c");
		catalog.addAll(v4, () -> Collections.singletonList(c), false);
		Constraint d = Constraint.forNode("C").named("c").exists("c");
		catalog.addAll(MigrationVersion.withValue("5"), () -> Collections.singletonList(d), true);

		assertThat(catalog.getItems()).containsExactly(d);

		assertThat(catalog.getItems(v1)).map(CatalogItem::getName).map(Name::getValue).containsExactly("cv1");

		assertThat(catalog.getItems(v2)).map(CatalogItem::getName).map(Name::getValue).containsExactly("cv1", "cv2");

		assertThat(catalog.getItems(v3)).isEmpty();

		assertThat(catalog.getItemsPriorTo(v3)).map(CatalogItem::getName)
			.map(Name::getValue)
			.containsExactly("cv1", "cv2");

		assertThat(catalog.getItemPriorTo(Name.of("cv1"), v3)).map(CatalogItem::getName)
			.map(Name::getValue)
			.hasValue("cv1");

		assertThat(catalog.getItems(v4)).containsExactly(c);

		assertThat(catalog.getItem(Name.of("c"), v4)).hasValue(c);

		assertThat(catalog.getCatalogAt(v2).getItems()).map(CatalogItem::getName)
			.map(Name::getValue)
			.containsExactly("cv1", "cv2");
	}

	@Test
	void getAllItemsPriorToShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);

		assertThat(catalog.getItemsPriorTo(MigrationVersion.withValue("1"))).isEmpty();

		assertThat(catalog.getItemsPriorTo(MigrationVersion.withValue("2"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.containsExactly("cv1");
	}

	@Test
	void getItemPriorToShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);

		assertThat(catalog.getItemPriorTo(Name.of("cv1"), MigrationVersion.withValue("1"))).isEmpty();

		assertThat(catalog.getItemPriorTo(Name.of("cv2"), MigrationVersion.withValue("1"))).isEmpty();

		assertThat(catalog.getItemPriorTo(Name.of("cv1"), MigrationVersion.withValue("2"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.hasValue("cv1");

		assertThat(catalog.getItemPriorTo(Name.of("cv2"), MigrationVersion.withValue("2"))).isEmpty();
	}

	@Test
	void getItemsShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);

		assertThat(catalog.getItems(MigrationVersion.withValue("1"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.containsExactly("cv1");

		assertThat(catalog.getItems(MigrationVersion.withValue("2"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.containsExactly("cv1", "cv2");
	}

	@Test
	void getItemShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
		catalog.addAll(MigrationVersion.withValue("1"), this.catalog1, false);
		catalog.addAll(MigrationVersion.withValue("2"), this.catalog2, false);

		assertThat(catalog.getItem(Name.of("cv1"), MigrationVersion.withValue("1"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.hasValue("cv1");

		assertThat(catalog.getItem(Name.of("cv2"), MigrationVersion.withValue("1"))).isEmpty();

		assertThat(catalog.getItem(Name.of("cv2"), MigrationVersion.withValue("2"))).map(CatalogItem::getName)
			.map(Name::getValue)
			.hasValue("cv2");
	}

	@Test
	void shouldThrowOnDuplicateVersion() {

		WriteableCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());

		MigrationVersion v1 = MigrationVersion.withValue("1");
		catalog.addAll(v1, this.catalog1, false);
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> catalog.addAll(v1, this.catalog2, false))
			.withMessage("A constraint with the name '%s' has already been added to this catalog under the version %s.",
					this.c1v1.getName().getValue(), v1.getValue());
	}

}
