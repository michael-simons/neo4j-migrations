/*
 * Copyright 2020-2024 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Defaults;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

/**
 * Part of the {@link MigrationsProperties} that can only be changed during build time.
 *
 * @author Michael J. Simons
 * @soundtrack Bad Religion - The Gray Race
 * @since 1.3.0
 */
@ConfigMapping(prefix = "org.neo4j.migrations")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface MigrationsBuildTimeProperties {

	/**
	 * List of packages to scan for Java migrations. This is a build time configuration option and can't be changed during runtime.
	 */
	@WithName("packages-to-scan")
	Optional<List<String>> packagesToScan();

	/**
	 * Locations of migrations scripts. This is a build time configuration option and can't be changed during runtime.
	 */
	@WithDefault(Defaults.LOCATIONS_TO_SCAN_VALUE)
	@WithName("locations-to-scan")
	List<String> locationsToScan();
}
