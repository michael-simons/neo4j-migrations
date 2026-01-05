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
package ac.simons.neo4j.migrations.maven;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.RepairmentResult;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal that repairs the configured database.
 *
 * @author Gerrit Meier
 * @since 2.2.0
 */
@Mojo(name = "repair", requiresDependencyResolution = ResolutionScope.TEST,
		defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class RepairMojo extends AbstractConnectedMojo {

	/**
	 * The default constructor is primarily used by the Maven machinery.
	 */
	public RepairMojo() {
		// Make both JDK 21 JavaDoc and Maven happy
	}

	@Override
	void withMigrations(Migrations migrations) {

		RepairmentResult result = migrations.repair();
		LOGGER.info(result::prettyPrint);
		result.getWarnings().forEach(LOGGER::warning);
	}

}
