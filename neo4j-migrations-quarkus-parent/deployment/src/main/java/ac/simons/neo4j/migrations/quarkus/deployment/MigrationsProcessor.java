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

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.Location;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsBuildTimeProperties;
import ac.simons.neo4j.migrations.quarkus.runtime.MigrationsRecorder;
import ac.simons.neo4j.migrations.quarkus.runtime.ResourceWrapper;
import ac.simons.neo4j.migrations.quarkus.runtime.StaticClasspathResourceScanner;
import ac.simons.neo4j.migrations.quarkus.runtime.StaticJavaBasedMigrationDiscoverer;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.neo4j.deployment.Neo4jDriverBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * This processor produces two additional items:
 * A synthetic bean of type {@link Migrations} and an additional bean of type {@link ServiceStartBuildItem}, the latter
 * indicating that all migrations have been applied (in case they are actually enabled).
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
public class MigrationsProcessor {

	static final String FEATURE_NAME = "neo4j-migrations";

	@BuildStep
	@SuppressWarnings("unused")
	FeatureBuildItem createFeature() {

		return new FeatureBuildItem(FEATURE_NAME);
	}

	static Set<Class<? extends JavaBasedMigration>> findClassBasedMigrations(Collection<String> packagesToScan, IndexView indexView) {

		if (packagesToScan.isEmpty()) {
			return Set.of();
		}

		var classesFoundAndLoaded = new HashSet<Class<? extends JavaBasedMigration>>();
		indexView
			.getAllKnownImplementations(DotName.createSimple(JavaBasedMigration.class.getName()))
			.forEach(cf -> {
				if (!packagesToScan.contains(cf.name().packagePrefix())) {
					return;
				}
				try {
					classesFoundAndLoaded.add(Thread.currentThread().getContextClassLoader()
						.loadClass(cf.name().toString()).asSubclass(JavaBasedMigration.class));
				} catch (ClassNotFoundException e) {
					// We silently ignore this (same behaviour as the Core-API does)
				}
			});
		return classesFoundAndLoaded;
	}

	@BuildStep
	@SuppressWarnings("unused")
	DiscovererBuildItem createDiscoverer(CombinedIndexBuildItem combinedIndexBuildItem, MigrationsBuildTimeProperties buildTimeProperties) {

		var packagesToScan = buildTimeProperties.packagesToScan().orElseGet(List::of);
		var classesFoundDuringBuild = findClassBasedMigrations(packagesToScan, combinedIndexBuildItem.getIndex());
		return new DiscovererBuildItem(StaticJavaBasedMigrationDiscoverer.of(classesFoundDuringBuild));
	}

	@BuildStep
	@SuppressWarnings("unused")
	ReflectiveClassBuildItem registerMigrationsForReflections(DiscovererBuildItem discovererBuildItem) {

		return ReflectiveClassBuildItem.builder(discovererBuildItem.getDiscoverer().getMigrationClasses().toArray(new Class<?>[0]))
			.constructors()
			.methods()
			.fields()
			.build();
	}

	static Set<ResourceWrapper> findResourceBasedMigrations(Collection<ResourceBasedMigrationProvider> providers, Collection<String> locationsToScan) throws IOException {

		if (locationsToScan.isEmpty()) {
			return Set.of();
		}

		var resourcesFound = new HashSet<ResourceWrapper>();
		var allSupportedExtensions = providers.stream()
			.map(ResourceBasedMigrationProvider::getExtension)
			.map(ext -> "." + ext)
			.collect(Collectors.toSet());

		Predicate<Path> isSupportedFile = path -> {
			if (!Files.isRegularFile(path)) {
				return false;
			}
			var fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
			return allSupportedExtensions.stream().anyMatch(fileName::endsWith);
		};

		// This piece is deliberately not using the streams due to the heckmeck with catching IOExceptions
		// and to avoid allocations of several sets.
		for (var value : locationsToScan) {
			var location = Location.of(value);
			if (location.getType() != Location.LocationType.CLASSPATH) {
				continue;
			}
			var name = location.getName();
			if (name.startsWith("/")) {
				name = name.substring(1);
			}
			var rootPath = Path.of(name);
			ClassPathUtils.consumeAsPaths(name, rootResource -> {
				try (var paths = Files.walk(rootResource)) {
					paths
						.filter(isSupportedFile)
						// Resolving the string and not the path object is done on purpose, as otherwise
						// a provider mismatch can occur.
						.map(it -> rootPath.resolve(rootResource.relativize(it).normalize().toString()))
						.map(r -> {
							var resource = new ResourceWrapper();
							resource.setUrl(r.toUri().toString());
							resource.setPath(r.toString().replace('\\', '/'));
							return resource;
						})
						.forEach(resourcesFound::add);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}

		return resourcesFound;
	}

	@BuildStep
	@SuppressWarnings("unused")
	ServiceProviderBuildItem resourceBasedMigrationProviderServices() {
		return ServiceProviderBuildItem.allProvidersFromClassPath(ResourceBasedMigrationProvider.class.getName());
	}

	@BuildStep
	@SuppressWarnings("unused")
	ClasspathResourceScannerBuildItem createScanner(MigrationsBuildTimeProperties buildTimeProperties) throws IOException {

		var providers = ResourceBasedMigrationProvider.unique();
		var resourcesFoundDuringBuild = findResourceBasedMigrations(providers, buildTimeProperties.locationsToScan());
		return new ClasspathResourceScannerBuildItem(StaticClasspathResourceScanner.of(resourcesFoundDuringBuild));
	}

	@BuildStep
	@SuppressWarnings("unused")
	NativeImageResourceBuildItem addCypherResources(

		ClasspathResourceScannerBuildItem classpathResourceScannerBuildItem) {
		return new NativeImageResourceBuildItem(
			classpathResourceScannerBuildItem.getScanner().getResources().stream().map(
				ResourceWrapper::getPath).toList());
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	@SuppressWarnings("unused")
	MigrationsBuildItem createMigrations(
		MigrationsBuildTimeProperties buildTimeProperties,
		DiscovererBuildItem discovererBuildItem,
		ClasspathResourceScannerBuildItem classpathResourceScannerBuildItem,
		MigrationsRecorder migrationsRecorder,
		Neo4jDriverBuildItem driverBuildItem,
		BuildProducer<SyntheticBeanBuildItem> syntheticBeans
	) {

		var configRv = migrationsRecorder.recordConfig(buildTimeProperties,
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
	@SuppressWarnings("unused")
	ServiceStartBuildItem applyMigrations(MigrationsRecorder migrationsRecorder, MigrationsBuildItem migrationsBuildItem) {
		migrationsRecorder.applyMigrations(migrationsBuildItem.getValue(),
			migrationsRecorder.isEnabled());
		return new ServiceStartBuildItem(FEATURE_NAME);
	}
}
