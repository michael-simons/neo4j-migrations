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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.Migrations;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

/**
 * Observer used to trigger a {@link Migrations Neo4j migration}.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@ApplicationScoped
public final class MigrationsInitializer {

	private static final Logger LOG = Logger.getLogger("ac.simons.neo4j.migrations.quarkus.runtime");

	private final Migrations migrations;

	private final MigrationsEnabled enabled;

	/**
	 * @param migrations The migrations for this application instance
	 * @param enabled    A flag whether to apply them or not during startup
	 */
	@Inject
	public MigrationsInitializer(Migrations migrations, MigrationsEnabled enabled) {
		this.migrations = migrations;
		this.enabled = enabled;
	}

	void onStart(@Observes StartupEvent ev) {

		if (!this.enabled.getAsBoolean()) {
			return;
		}
		try {
			migrations.apply();
		} catch (ServiceUnavailableException e) {
			LOG.error("Cannot apply Neo4j migrations, driver instance cannot reach any database.", e);
		}
	}
}
