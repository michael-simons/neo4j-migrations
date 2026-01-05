/*
 * Copyright 2020-2026 the original author or authors.
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
 * Provides detailed information about the connection being used when invoking any method
 * that talks to the database.
 *
 * @author Michael J. Simons
 * @since 1.4.0
 */
public sealed interface ConnectionDetails permits MigrationChain, DefaultConnectionDetails {

	/**
	 * Creates an instance of {@link ConnectionDetails}. Can be useful for testing.
	 * @param serverAddress the address of the server used
	 * @param serverVersion the neo4j version the server is running
	 * @param serverEdition the neo4j edition the server is running
	 * @param userName the neo4j user that ran the migrations
	 * @param optionalDatabaseName the database if applicable (Neo4j 4.0 and up)
	 * @param optionalSchemaDatabaseName the database if applicable (Neo4j 4.0 and up)
	 * @return a new, unmodifiable instance
	 * @since 2.3.0
	 */
	static ConnectionDetails of(String serverAddress, String serverVersion, String serverEdition, String userName,
			String optionalDatabaseName, String optionalSchemaDatabaseName) {
		return new DefaultConnectionDetails(serverAddress, serverVersion, serverEdition, userName, optionalDatabaseName,
				optionalSchemaDatabaseName);
	}

	/**
	 * {@return the address of the server used}
	 */
	String getServerAddress();

	/**
	 * {@return the Neo4j version the server is running}
	 */
	String getServerVersion();

	/**
	 * {@return the Neo4j edition the server is running}
	 * @since 1.5.0
	 */
	String getServerEdition();

	/**
	 * {@return the Neo4j user that ran the migrations}
	 */
	String getUsername();

	/**
	 * {@return the database if applicable (Neo4j 4.0 and up)}
	 */
	Optional<String> getOptionalDatabaseName();

	/**
	 * {@return the database if applicable (Neo4j 4.0 and up)}
	 */
	Optional<String> getOptionalSchemaDatabaseName();

}
