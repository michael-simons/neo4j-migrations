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

import java.util.Map;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.Migrations;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

/**
 * A service delegating to an instance of {@link Migrations migrations} for various tasks in the dev-ui.
 *
 * @author Michael J. Simons
 */
public final class MigrationsRPCService {

	private final Migrations migrations;

	@Inject
	public MigrationsRPCService(Migrations migrations) {
		this.migrations = migrations;
	}

	public String getLabel() {
		return String.format("%s (%s)", migrations.getConnectionDetails().getServerAddress(), migrations.getConnectionDetails().getOptionalDatabaseName().orElse("default database"));
	}

	public JsonObject getConnectionDetails() {

		var connectionDetails = migrations.getConnectionDetails();
		var result = new JsonObject();
		result.put("username", connectionDetails.getUsername());
		result.put("serverAddress", connectionDetails.getServerAddress());
		result.put("serverVersion", connectionDetails.getServerVersion());
		connectionDetails.getOptionalDatabaseName().ifPresent(name -> result.put("database", name));
		connectionDetails.getOptionalSchemaDatabaseName().ifPresent(name -> result.put("schemaDatabase", name));
		return result;
	}

	public JsonArray getAll() {

		var result = new JsonArray();
		var elements = migrations.info().getElements();
		for (MigrationChain.Element element : elements) {
			result.add(new JsonObject(Map.copyOf(element.asMap())));
		}
		return result;
	}

	public JsonObject migrate() {

		var result = new JsonObject();
		var message = this.migrations.apply().map(version -> "Database migrated to  " + version.getValue())
			.orElse("No change");
		result.put("message", message);
		result.put("elements", getAll());
		return result;
	}

	public JsonObject clean() {

		var result = new JsonObject();
		result.put("message", this.migrations.clean(false).prettyPrint());
		result.put("elements", getAll());
		return result;
	}
}
