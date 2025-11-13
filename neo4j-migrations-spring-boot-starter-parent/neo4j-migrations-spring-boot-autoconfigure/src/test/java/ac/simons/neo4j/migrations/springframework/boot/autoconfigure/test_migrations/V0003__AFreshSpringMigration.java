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
package ac.simons.neo4j.migrations.springframework.boot.autoconfigure.test_migrations;

import java.util.Map;
import java.util.Objects;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import org.neo4j.driver.Session;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Example for a Spring based Java migration.
 */
@Component
public class V0003__AFreshSpringMigration implements JavaBasedMigration {

	private final Environment environment;

	public V0003__AFreshSpringMigration(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void apply(MigrationContext context) {
		try (Session session = context.getSession()) {

			var property = Objects.requireNonNull(this.environment.getProperty("spring.application.name"));
			session.run("CREATE (onA:WALL{graffito: 'Spring is in the air', appName: $appName})",
					Map.of("appName", property));
		}
	}

}
