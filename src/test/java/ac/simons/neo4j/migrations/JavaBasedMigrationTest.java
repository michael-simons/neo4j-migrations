/*
 * Copyright 2020 the original author or authors.
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
package ac.simons.neo4j.migrations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 */
class JavaBasedMigrationTest {

	@Test
	void shouldGetCorrectVersion() {

		Assertions.assertEquals("001", new V001__SomeMigration().getVersion().getValue());
	}

	@Test
	void shouldGetCorrectDescription() {

		Assertions.assertEquals("SomeMigration", new V001__SomeMigration().getDescription());
		Assertions.assertEquals("V002__SomeWhatBroken", new V002__SomeWhatBroken().getDescription());
	}

	private static class V001__SomeMigration implements JavaBasedMigration {

		@Override
		public void apply(Driver driver) {
			throw new UnsupportedOperationException();
		}
	}

	private static class V002__SomeWhatBroken implements JavaBasedMigration {

		@Override
		public MigrationVersion getVersion() {
			return MigrationVersion.withValue("lol");
		}

		@Override
		public void apply(Driver driver) {
			throw new UnsupportedOperationException();
		}
	}
}
