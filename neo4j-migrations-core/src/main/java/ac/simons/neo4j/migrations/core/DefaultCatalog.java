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

import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.ItemType;
import ac.simons.neo4j.migrations.core.catalog.TargetEntity;

import java.util.List;
import java.util.Objects;

/**
 * @author Michael J. Simons
 * @since TBA
 */
class DefaultCatalog implements WriteableCatalog {

	/**
	 * Represents the id of an entry in the catalog. This takes the original id and adds the version to id.
	 *
	 * @author Michael J. Simons
	 * @soundtrack Metallica - Ride The Lightning
	 * @since TBA
	 */
	static final class CatalogId {

		private final String value;

		private final MigrationVersion version;

		CatalogId(String value, MigrationVersion version) {
			this.value = value;
			this.version = version;
		}

		String getValue() {
			return value;
		}

		MigrationVersion getVersion() {
			return version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CatalogId catalogId = (CatalogId) o;
			return value.equals(catalogId.value) && version.equals(catalogId.version);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, version);
		}
	}

	@Override
	public void addAll(MigrationVersion version, List<Constraint> newConstraints) {
		ItemType type = newConstraints.get(0).getType();
		TargetEntity target = newConstraints.get(0).getTarget();
	}

	@Override
	public List<Constraint> getConstraints() {
		throw new UnsupportedOperationException("Not there yet");
	}
}
