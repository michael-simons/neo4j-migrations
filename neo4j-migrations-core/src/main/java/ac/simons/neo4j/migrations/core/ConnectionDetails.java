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
package ac.simons.neo4j.migrations.core;

import java.util.Optional;

/**
 * Provides detailed information about the connection being used when invoking any method that talks to the database.
 *
 * @author Michael J. Simons
 * @soundtrack Snoop Dogg - Doggystyle
 * @since 1.4.0
 */
public interface ConnectionDetails {

	/**
	 * Neo4j edition
	 */
	enum Edition {
		ENTERPRISE("enterprise"),
		COMMUNITY("community");

		private final String edition;

		Edition(String edition) {
			this.edition = edition;
		}

		public static Edition from(String editionValue) {
			for (Edition edition : values()) {
				if (edition.edition.equals(editionValue)) {
					return edition;
				}
			}
			return null; // todo or fail
		}
	}

	/**
	 * @return the address of the server used
	 */
	String getServerAddress();

	/**
	 * @return the Neo4j version the server is running
	 */
	String getServerVersion();

	/**
	 * @return the Neo4j user that ran the migrations
	 */
	String getUsername();

	/**
	 * @return the database if applicable (Neo4j 4.0 and up)
	 */
	Optional<String> getOptionalDatabaseName();

	/**
	 * @return the database if applicable (Neo4j 4.0 and up)
	 */
	Optional<String> getOptionalSchemaDatabaseName();

	/**
	 * @return the Neo4j {@link Edition}
	 */
	Edition getEdition();
}
