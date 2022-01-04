/*
 * Copyright 2020-2022 the original author or authors.
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
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsBuildTimeProperties;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsProperties;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsRecorder;
import ac.simons.neo4j.migrations.quarkus.runtime.ResourceWrapper;
import ac.simons.neo4j.migrations.quarkus.runtime.StaticClasspathResourceScanner;
import ac.simons.neo4j.migrations.quarkus.runtime.StaticJavaBasedMigrationDiscoverer;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.neo4j.deployment.Neo4jDriverBuildItem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This processor produces two additional items:
 * A synthetic bean of type {@link Migrations} and an additional bean of type {@link ServiceStartBuildItem}, the latter
 * indicating that all migrations have been applied (in case they are actually enabled).
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
class MigrationsProcessor {

	public static final String FEATURE_NAME = "neo4j-migrations";

	@BuildStep
	FeatureBuildItem createFeature() {

		return new FeatureBuildItem(FEATURE_NAME);
	}

	@BuildStep
	DiscovererBuildItem createDiscoverer(MigrationsBuildTimeProperties buildTimeProperties) {
		return new DiscovererBuildItem(
			StaticJavaBasedMigrationDiscoverer.from(buildTimeProperties.packagesToScan.orElseGet(List::of)));
	}

	@BuildStep
	ReflectiveClassBuildItem registerMigrationsForReflections(DiscovererBuildItem discovererBuildItem) {
		return new ReflectiveClassBuildItem(true, true, true,
			discovererBuildItem.getDiscoverer().getMigrationClasses().toArray(new Class<?>[0]));
	}

	@BuildStep
	ClasspathResourceScannerBuildItem createScanner(MigrationsBuildTimeProperties buildTimeProperties) {
		return new ClasspathResourceScannerBuildItem(
			StaticClasspathResourceScanner.from(buildTimeProperties.locationsToScan));
	}

	@BuildStep
	NativeImageResourceBuildItem addCypherResources(
		ClasspathResourceScannerBuildItem classpathResourceScannerBuildItem) {
		return new NativeImageResourceBuildItem(
			classpathResourceScannerBuildItem.getScanner().getResources().stream().map(
				ResourceWrapper::getPath).collect(
				Collectors.toList()));
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	MigrationsBuildItem createMigrations(
		MigrationsBuildTimeProperties buildTimeProperties,
		MigrationsProperties runtimeProperties,
		DiscovererBuildItem discovererBuildItem,
		ClasspathResourceScannerBuildItem classpathResourceScannerBuildItem,
		MigrationsRecorder migrationsRecorder,
		Neo4jDriverBuildItem driverBuildItem,
		BuildProducer<SyntheticBeanBuildItem> syntheticBeans
	) {
		var configRv = migrationsRecorder.recordConfig(buildTimeProperties, runtimeProperties,
			discovererBuildItem.getDiscoverer(),
			classpathResourceScannerBuildItem.getScanner());
		syntheticBeans.produce(
			SyntheticBeanBuildItem.configure(MigrationsConfig.class).runtimeValue(configRv).setRuntimeInit().done());

		var migrationsRv = migrationsRecorder.recordMigrations(configRv, driverBuildItem.getValue());
		syntheticBeans.produce(
			SyntheticBeanBuildItem.configure(Migrations.class).runtimeValue(migrationsRv).setRuntimeInit().done());

		return new MigrationsBuildItem(migrationsRv);
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	ServiceStartBuildItem applyMigrations(MigrationsProperties migrationsProperties,
		MigrationsRecorder migrationsRecorder, MigrationsBuildItem migrationsBuildItem) {
		migrationsRecorder.applyMigrations(migrationsBuildItem.getValue(),
			migrationsRecorder.isEnabled(migrationsProperties));
		return new ServiceStartBuildItem(FEATURE_NAME);
	}
}
