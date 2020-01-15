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
package ac.simons.neo4j.migrations.core;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * Parameter object / context for migrations to be applied.
 *
 * @author Michael J. Simons
 * @since 0.0.2
 */
public interface MigrationContext {

	/**
	 * @return The configuration of this migration instance.
	 */
	MigrationsConfig getConfig();

	/**
	 * @return The driver to be used inside migrations.
	 */
	Driver getDriver();

	/**
	 * Use this session config in your Java based migration to make sure you use the same database as the migration tool itself.
	 *
	 * @return A correctly configured write access session config.
	 */
	SessionConfig getSessionConfig();

	/**
	 * Convenience method to return an imperative session against the configured server and database.
	 *
	 * @return A session configured to use the migration context's database name.
	 */
	default Session getSession() {

		return getDriver().session(getSessionConfig());
	}
}
