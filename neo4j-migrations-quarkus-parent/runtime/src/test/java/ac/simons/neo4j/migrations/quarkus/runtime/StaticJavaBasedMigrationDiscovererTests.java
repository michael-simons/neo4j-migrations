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
package ac.simons.neo4j.migrations.quarkus.runtime;

import java.util.Set;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class StaticJavaBasedMigrationDiscovererTests {

	@Test
	void shouldStoreClass() {

		class Hlp implements JavaBasedMigration {

			@Override
			public void apply(MigrationContext context) {
				// Class needed for its own sake, not for the implementation.
			}

		}

		var discoverer = new StaticJavaBasedMigrationDiscoverer();
		discoverer.setMigrationClasses(Set.of(Hlp.class));
		assertThat(discoverer.getMigrationClasses()).containsExactly(Hlp.class);
	}

}
