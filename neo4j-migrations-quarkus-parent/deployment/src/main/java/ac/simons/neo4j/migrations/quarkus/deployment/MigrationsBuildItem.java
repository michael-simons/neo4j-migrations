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
package ac.simons.neo4j.migrations.quarkus.deployment;

import ac.simons.neo4j.migrations.core.Migrations;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * A Neo4j-Migrations built-time item to be consumed by interested parties during their
 * deployment.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
final class MigrationsBuildItem extends SimpleBuildItem {

	private final RuntimeValue<Migrations> value;

	MigrationsBuildItem(RuntimeValue<Migrations> value) {
		this.value = value;
	}

	/**
	 * {@return the runtime value containing the migration instance}
	 */
	RuntimeValue<Migrations> getValue() {
		return this.value;
	}

}
