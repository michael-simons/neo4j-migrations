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
package ac.simons.neo4j.migrations.quarkus.runtime;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * Config customizer to ignore validation of unmapped properties between build-time and
 * runtime.
 *
 * @author Michael J. Simons
 * @author Roberto Cortez
 * @see MigrationsProperties
 * @see MigrationsBuildTimeProperties
 */
@StaticInitSafe
public final class MigrationsConfigCustomizer implements SmallRyeConfigBuilderCustomizer {

	@Override
	public void configBuilder(final SmallRyeConfigBuilder builder) {
		builder.withMappingIgnore("org.neo4j.migrations.**");
	}

}
