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
package ac.simons.neo4j.migrations.core;

import java.util.Optional;

/**
 * Utility for formatting a {@link ConnectionDetails connection details object} in a
 * uniform way.
 *
 * @author Michael J. Simons
 * @since 1.12.0
 */
enum ConnectionDetailsFormatter {

	INSTANCE;

	String format(ConnectionDetails cd) {
		StringBuilder sb = new StringBuilder();
		sb.append(MigrationChainFormat.LS)
			.append(cd.getUsername())
			.append("@")
			.append(cd.getServerAddress())
			.append(" (")
			.append(cd.getServerVersion());
		if (cd.getServerEdition() != null) {
			sb.append(" ").append(cd.getServerEdition()).append(" Edition)");
		}
		else {
			sb.append(")");
		}

		Optional<String> optionalDatabase = cd.getOptionalDatabaseName();
		optionalDatabase.ifPresent(name -> sb.append(MigrationChainFormat.LS).append("Database: ").append(name));

		Optional<String> optionalSchemaDatabase = cd.getOptionalSchemaDatabaseName();
		if (!optionalSchemaDatabase.equals(optionalDatabase)) {
			optionalSchemaDatabase
				.ifPresent(name -> sb.append(MigrationChainFormat.LS).append("Schema database: ").append(name));
		}
		return sb.toString();
	}

}
