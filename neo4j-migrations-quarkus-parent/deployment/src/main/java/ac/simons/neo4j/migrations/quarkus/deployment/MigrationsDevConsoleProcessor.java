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

import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsRPCService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Provides the necessary RPC items to fill toe web-ui, including the corresponding cards.
 *
 * @author Michael J. Simons
 * @since 1.4.0
 */
public class MigrationsDevConsoleProcessor {

	@BuildStep(onlyIf = IsDevelopment.class)
	@SuppressWarnings("unused")
	CardPageBuildItem createMigrationsCard(MigrationsBuildItem migrationsBuildItem) {

		var card = new CardPageBuildItem();
		card.addPage(Page.webComponentPageBuilder()
			.title("All migrations")
			.componentLink("qwc-neo4j-migrations.js")
			.icon("font-awesome-solid:database")
			.dynamicLabelJsonRPCMethodName("getLabel"));

		return card;
	}

	@BuildStep(onlyIf = IsDevelopment.class)
	@SuppressWarnings("unused")
	JsonRPCProvidersBuildItem createMigrationsRPCService() {
		return new JsonRPCProvidersBuildItem(MigrationsRPCService.class);
	}

}
