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
package ac.simons.neo4j.migrations.test_migrations.changeset1;

import ac.simons.neo4j.migrations.JavaBasedMigration;
import ac.simons.neo4j.migrations.MigrationContext;

import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
public class V001__FirstMigration implements JavaBasedMigration {

	@SuppressWarnings("unused")
	@Override
	public void apply(MigrationContext context) {

		try (Session session = context.getDriver().session(context.getSessionConfig())) {
			// Steps necessary for a migration
		}
	}

}
