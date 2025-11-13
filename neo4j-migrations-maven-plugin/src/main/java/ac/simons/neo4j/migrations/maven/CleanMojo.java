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

import ac.simons.neo4j.migrations.core.CleanResult;
import ac.simons.neo4j.migrations.core.Migrations;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal that cleans the configured database. Can be configured with {@code all} set to
 * {@literal true} for removing everything Neo4j-Migrations created in a schema database.
 * Binds to the clean phase by default
 *
 * @author Michael J. Simons
 * @since 1.1.0
 */
@Mojo(name = "clean", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.CLEAN,
		threadSafe = true)
public class CleanMojo extends AbstractConnectedMojo {

	/**
	 * Set to true to delete all migration chains as well as all Neo4j-Migration
	 * constraints and not only the chain for the target database.
	 */
	@Parameter(defaultValue = "false")
	private boolean all;

	/**
	 * The default constructor is primarily used by the Maven machinery.
	 */
	public CleanMojo() {
		// Make both JDK 21 JavaDoc and Maven happy
	}

	@Override
	void withMigrations(Migrations migrations) {

		CleanResult result = migrations.clean(this.all);
		LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(LOGGER::warning);
	}

}
