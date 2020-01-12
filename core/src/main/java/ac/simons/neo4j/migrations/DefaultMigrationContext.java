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

import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 */
final class DefaultMigrationContext implements MigrationContext {

	private final MigrationsConfig config;

	private final Driver driver;

	DefaultMigrationContext(MigrationsConfig config, Driver driver) {
		this.config = config;
		this.driver = driver;
	}

	@Override
	public MigrationsConfig getConfig() {
		return config;
	}

	@Override
	public Driver getDriver() {
		return driver;
	}
}
