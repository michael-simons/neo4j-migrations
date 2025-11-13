/*
 * Copyright 2020-2025 the original author or authors.
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
package ac.simons.neo4j.migrations.maven;

import java.util.Optional;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.MigrationVersion;
import ac.simons.neo4j.migrations.core.Migrations;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal that applies the configured migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.11
 */
@Mojo(name = "migrate", requiresDependencyResolution = ResolutionScope.TEST,
		defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class MigrateMojo extends AbstractConnectedMojo {

	/**
	 * The default constructor is primarily used by the Maven machinery.
	 */
	public MigrateMojo() {
		// Make both JDK 21 JavaDoc and Maven happy
	}

	@Override
	void withMigrations(Migrations migrations) {

		Optional<MigrationVersion> lastAppliedMigration = migrations.apply();
		lastAppliedMigration.map(MigrationVersion::getValue)
			.ifPresent(version -> LOGGER.log(Level.INFO, "Database migrated to version {0}.", version));
	}

}
