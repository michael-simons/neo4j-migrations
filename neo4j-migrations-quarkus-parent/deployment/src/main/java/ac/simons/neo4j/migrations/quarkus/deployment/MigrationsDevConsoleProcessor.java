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
package ac.simons.neo4j.migrations.quarkus.deployment;

import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsRPCService;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsDevConsoleRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * @author Michael J. Simons
 * @soundtrack Dr. Dre - The Chronic
 * @since 1.4.0
 */
public class MigrationsDevConsoleProcessor {

	@BuildStep(onlyIf = IsDevelopment.class)
	@Record(ExecutionTime.RUNTIME_INIT)
	@SuppressWarnings("unused")
	DevConsoleRuntimeTemplateInfoBuildItem addMigrationsInfo(
		MigrationsBuildItem migrationsBuildItem,
		CurateOutcomeBuildItem curateOutcomeBuildItem,
		MigrationsDevConsoleRecorder recorder
	) {
		return new DevConsoleRuntimeTemplateInfoBuildItem("migrations", recorder.recordMigrationsSupplier(
			migrationsBuildItem.getValue()), this.getClass(),
			curateOutcomeBuildItem);
	}

	@BuildStep
	@Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
	@SuppressWarnings("unused")
	DevConsoleRouteBuildItem addMigrationsRoute(
		MigrationsBuildItem migrationsBuildItem,
		MigrationsDevConsoleRecorder recorder
	) {
		return new DevConsoleRouteBuildItem("migrations", "POST",
			recorder.recordHandler(migrationsBuildItem.getValue()));
	}

	@BuildStep(onlyIf = IsDevelopment.class)
	@SuppressWarnings("unused")
	CardPageBuildItem createMigrationsCard(MigrationsBuildItem migrationsBuildItem) {

		var card = new CardPageBuildItem();
		card.addPage(
			Page.webComponentPageBuilder()
				.title("All migrations")
				.componentLink("qwc-neo4j-migrations.js")
				.icon("font-awesome-solid:database")
				.dynamicLabelJsonRPCMethodName("getLabel")
		);

		return card;
	}

	@BuildStep(onlyIf = IsDevelopment.class)
	@SuppressWarnings("unused")
	JsonRPCProvidersBuildItem createMigrationsRPCService() {
		return new JsonRPCProvidersBuildItem(MigrationsRPCService.class);
	}
}
