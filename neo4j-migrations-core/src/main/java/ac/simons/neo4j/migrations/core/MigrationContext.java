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

import java.util.function.UnaryOperator;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
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
	 * This creates a {@link MigrationContext migration context} outside of an otherwise
	 * configured {@link Migrations migration instance}. It will behave exactly as the
	 * context handed out by migrations to {@link JavaBasedMigration class based
	 * migrations},
	 * @param config the configuration to use
	 * @param driver the instance of the driver to use, might as well be a mock
	 * @return a new context, independent of any other migration instance or configuration
	 * @since 1.16.0
	 */
	static MigrationContext of(MigrationsConfig config, Driver driver) {
		return new DefaultMigrationContext(config, driver);
	}

	/**
	 * {@return the configuration of this migration instance}
	 */
	MigrationsConfig getConfig();

	/**
	 * {@return the driver to be used inside migrations}
	 */
	Driver getDriver();

	/**
	 * Use this session config in your Java-based migration to make sure you use the same
	 * database as the migration tool itself.
	 * @return a correctly configured write access session config.
	 */
	SessionConfig getSessionConfig();

	/**
	 * This method provides a callback that will be applied to the
	 * {@link SessionConfig.Builder} just before a {@link SessionConfig} is created.
	 * @param configCustomizer customization callback for the builder.
	 * @return the final session config.
	 * @since 1.1.0
	 */
	SessionConfig getSessionConfig(UnaryOperator<SessionConfig.Builder> configCustomizer);

	/**
	 * Convenience method to return an imperative session against the configured server
	 * and database.
	 * @return a session configured to use the migration context's database name
	 */
	default Session getSession() {

		return getDriver().session(getSessionConfig());
	}

	/**
	 * The schema database might or might not be identical with the database that is to be
	 * migrated. The schema database contains the objects that neo4j-migrations requires
	 * to work.
	 * @return a session accessing the configured schema database if any or the default
	 * database
	 */
	Session getSchemaSession();

	/**
	 * The details will give detailed information about the user being connected, server
	 * address and which databases - if available - are being migrated and which are used
	 * to store the schema.
	 * @return details about the connection being used within the context of migrations
	 * @since 1.4.0
	 */
	ConnectionDetails getConnectionDetails();

	/**
	 * {@return the catalog known to this context}
	 */
	Catalog getCatalog();

}
