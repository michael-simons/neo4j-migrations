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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Id;
import ac.simons.neo4j.migrations.core.catalog.Name;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Michael J. Simons
 */
class DefaultCatalogTest {

	Constraint c1v1;

	Constraint c1v2;

	Catalog catalog1;

	Catalog catalog2;

	@BeforeEach
	void initMocks() {

		c1v1 = Mockito.mock(Constraint.class);
		Name name1 = Name.of("cv1");
		when(c1v1.getId()).thenReturn(name1);
		when(c1v1.getName()).thenReturn(name1);
		when(c1v1.getProperties()).thenReturn(new HashSet<>(Arrays.asList("a", "b")));

		c1v2 = mock(Constraint.class);
		when(c1v2.getId()).thenReturn(name1);
		when(c1v2.getName()).thenReturn(name1);
		when(c1v2.getProperties()).thenReturn(new HashSet<>(Arrays.asList("a", "b", "c")));

		Constraint c2v1 = mock(Constraint.class);
		Name name2 = Name.of("cv2");
		when(c2v1.getId()).thenReturn(name2);
		when(c2v1.getName()).thenReturn(name2);
		when(c2v1.getProperties()).thenReturn(Collections.singleton("d"));

		catalog1 = mock(Catalog.class);
		when(catalog1.getConstraints()).thenReturn(Collections.singletonList(c1v1));

		catalog2 = mock(Catalog.class);
		when(catalog2.getConstraints()).thenReturn(Arrays.asList(c1v2, c2v1));
	}

	@Test
	void getAllConstraintsShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog();
		catalog.addAll(MigrationVersion.withValue("1"), catalog1);
		catalog.addAll(MigrationVersion.withValue("2"), catalog2);

		assertThat(catalog.getConstraints())
			.map(Constraint::getId)
			.map(Id::getValue)
			.containsExactly("cv1", "cv2");
	}

	@Test
	void getAllConstraintsPriorToShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog();
		catalog.addAll(MigrationVersion.withValue("1"), catalog1);
		catalog.addAll(MigrationVersion.withValue("2"), catalog2);

		assertThat(catalog.getConstraintsPriorTo(MigrationVersion.withValue("1"))).isEmpty();

		assertThat(catalog.getConstraintsPriorTo(MigrationVersion.withValue("2")))
			.map(Constraint::getId)
			.map(Id::getValue)
			.containsExactly("cv1");
	}

	@Test
	void getConstraintPriorToShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog();
		catalog.addAll(MigrationVersion.withValue("1"), catalog1);
		catalog.addAll(MigrationVersion.withValue("2"), catalog2);

		assertThat(catalog.getConstraintPriorTo(Name.of("cv1"), MigrationVersion.withValue("1")))
			.isEmpty();

		assertThat(catalog.getConstraintPriorTo(Name.of("cv2"), MigrationVersion.withValue("1")))
			.isEmpty();

		assertThat(catalog.getConstraintPriorTo(Name.of("cv1"), MigrationVersion.withValue("2")))
			.map(Constraint::getId).map(Id::getValue).hasValue("cv1");

		assertThat(catalog.getConstraintPriorTo(Name.of("cv2"), MigrationVersion.withValue("2")))
			.isEmpty();
	}

	@Test
	void getConstraintsShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog();
		catalog.addAll(MigrationVersion.withValue("1"), catalog1);
		catalog.addAll(MigrationVersion.withValue("2"), catalog2);

		assertThat(catalog.getConstraints(MigrationVersion.withValue("1")))
			.map(Constraint::getId)
			.map(Id::getValue)
			.containsExactly("cv1");

		assertThat(catalog.getConstraints(MigrationVersion.withValue("2")))
			.map(Constraint::getId)
			.map(Id::getValue)
			.containsExactly("cv1", "cv2");
	}

	@Test
	void getConstraintShouldWork() {

		DefaultCatalog catalog = new DefaultCatalog();
		catalog.addAll(MigrationVersion.withValue("1"), catalog1);
		catalog.addAll(MigrationVersion.withValue("2"), catalog2);

		assertThat(catalog.getConstraint(Name.of("cv1"), MigrationVersion.withValue("1")))
			.map(Constraint::getId).map(Id::getValue).hasValue("cv1");

		assertThat(catalog.getConstraint(Name.of("cv2"), MigrationVersion.withValue("1")))
			.isEmpty();

		assertThat(catalog.getConstraint(Name.of("cv2"), MigrationVersion.withValue("2")))
			.map(Constraint::getId).map(Id::getValue).hasValue("cv2");
	}

	@Test
	void shouldThrowOnDuplicateVersion() {

		WriteableCatalog catalog = new DefaultCatalog();

		MigrationVersion v1 = MigrationVersion.withValue("1");
		catalog.addAll(v1, catalog1);
		assertThatExceptionOfType(MigrationsException.class)
			.isThrownBy(() -> catalog.addAll(v1, catalog2))
			.withMessage("A constraint with the id '%s' has already been added to this catalog under the version %s.",
				c1v1.getName().getValue(), v1.getValue());
	}
}
