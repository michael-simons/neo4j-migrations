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
package ac.simons.neo4j.migrations.cli;

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine.IVersionProvider;

/**
 * Version provider modelled after the official example <a href=
 * "https://github.com/remkop/picocli/blob/master/picocli-examples/src/main/java/picocli/examples/VersionProviderDemo2.java">here</a>
 * Implementation has been moved to the core module, see
 * {@literal ac.simons.neo4j.migrations.core.ProductionVersion}.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
final class ManifestVersionProvider implements IVersionProvider {

	@Override
	public String[] getVersion() {
		return new String[] { Migrations.getUserAgent() };
	}

}
