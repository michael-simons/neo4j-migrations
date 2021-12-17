/*
 * Copyright 2020-2021 the original author or authors.
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
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsInitializer;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsProperties;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.neo4j.deployment.Neo4jDriverBuildItem;

/**
 * This processor produces two additional items:
 * A synthetic bean of type {@link Migrations} and an additional bean of type {@link MigrationsInitializer}, the latter
 * observing the start of an application and migration the database.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
class MigrationsProcessor {

	@BuildStep
	FeatureBuildItem createFeature() {

		return new FeatureBuildItem("neo4j-migrations");
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	MigrationsBuildItem migrationsStep(
		MigrationsProperties migrationsProperties,
		MigrationsRecorder migrationsRecorder,
		Neo4jDriverBuildItem driverBuildItem,
		BuildProducer<SyntheticBeanBuildItem> syntheticBeans
	) {
		var configRv = migrationsRecorder.initializeConfig(migrationsProperties);
		syntheticBeans.produce(
			SyntheticBeanBuildItem.configure(MigrationsConfig.class).runtimeValue(configRv).setRuntimeInit().done());

		var migrationsRv = migrationsRecorder.initializeMigrations(configRv, driverBuildItem.getValue());
		syntheticBeans.produce(
			SyntheticBeanBuildItem.configure(Migrations.class).runtimeValue(migrationsRv).setRuntimeInit().done());

		return new MigrationsBuildItem(migrationsRv);
	}

	@BuildStep
	void migrationsInitializerStep(MigrationsBuildTimeConfig buildTimeConfig,
		BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
		if (!buildTimeConfig.enabled) {
			return;
		}
		additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(MigrationsInitializer.class));
	}
}
