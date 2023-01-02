/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.quarkus.it;

import ac.simons.neo4j.migrations.core.Migrations;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Returning a list of migrations.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@Path("/migrations")
public class MigrationsResource {

	@Inject
	Migrations migrations;

	/**
	 * @return The list of migrations given by the info command.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> get() {
		return migrations.info().getElements().stream()
			.map(e -> e.getVersion() + ": " + e.getOptionalDescription().orElse("n/a") + " (" + e.getState() + ")")
			.toList();

	}
}
