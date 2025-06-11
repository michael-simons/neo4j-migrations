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
package ac.simons.neo4j.migrations.quarkus.runtime;

import ac.simons.neo4j.migrations.core.Migrations;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import java.util.Locale;

/**
 * Handles {@literal POST} requests from the dev-ui.
 *
 * @author Michael J. Simons
 * @soundtrack Dr. Dre - The Chronic
 * @since 1.3.4
 */
public class MigrationsDevConsoleHandler extends DevConsolePostHandler {

	private final Migrations migrations;

	MigrationsDevConsoleHandler(Migrations migrations) {
		this.migrations = migrations;
	}

	/**
	 * Reactions to {@literal clean} and {@literal apply} operations.
	 *
	 * @param event the context event
	 * @param form  the form data
	 */
	@Override
	protected void handlePost(RoutingContext event, MultiMap form) {

		var operation = form.get("operation");
		switch (operation.toLowerCase(Locale.ROOT)) {
			case "clean":
				var result = migrations.clean(false);
				flashMessage(event, result.prettyPrint());
				break;
			case "apply":
				migrations.apply()
					.ifPresent(version -> flashMessage(event, "Database migrated to  " + version.getValue()));
				break;
			default:
				throw new UnsupportedOperationException(operation);
		}
	}
}
