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
import ac.simons.neo4j.migrations.core.CypherResourceBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.DefaultCatalogBasedMigrationProvider;
import ac.simons.neo4j.migrations.core.ResourceBasedMigrationProvider;

@SuppressWarnings({ "requires-automatic", "requires-transitive-automatic" }) // needed for org.neo4j.driver
module ac.simons.neo4j.migrations.core {

	requires io.github.classgraph;

	requires transitive java.logging;

	requires transitive org.neo4j.driver;

	exports ac.simons.neo4j.migrations.core;
	exports ac.simons.neo4j.migrations.core.catalog;
	exports ac.simons.neo4j.migrations.core.refactorings;

	exports ac.simons.neo4j.migrations.core.internal to
		ac.simons.neo4j.migrations.cli, ac.simons.neo4j.migrations.maven,
		ac.simons.neo4j.migrations.quarkus.deployment, ac.simons.neo4j.migrations.quarkus.runtime;

	provides ResourceBasedMigrationProvider with CypherResourceBasedMigrationProvider, DefaultCatalogBasedMigrationProvider;
}
