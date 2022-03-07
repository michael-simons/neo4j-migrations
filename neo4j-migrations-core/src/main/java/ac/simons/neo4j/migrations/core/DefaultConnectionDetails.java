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

import ac.simons.neo4j.migrations.core.internal.Strings;

import java.util.Optional;

/**
 * @author Michael J. Simons
 * @soundtrack Snoop Dogg - Doggystyle
 * @since 1.4.0
 */
final class DefaultConnectionDetails implements ConnectionDetails {

	private final String serverAddress;

	private final String serverVersion;

	private final String edition;

	private final String username;

	private final String databaseName;

	private final String schemaDatabaseName;

	DefaultConnectionDetails(String serverAddress, String serverVersion, String edition, String username,
		String databaseName, String schemaDatabaseName) {
		this.serverAddress = serverAddress;
		this.serverVersion = serverVersion;
		this.edition = Strings.capitalize(edition);
		this.username = username;
		this.databaseName = databaseName;
		this.schemaDatabaseName = schemaDatabaseName;
	}

	@Override
	public String getServerAddress() {
		return serverAddress;
	}

	@Override
	public String getServerVersion() {
		return serverVersion;
	}

	@Override
	public String getServerEdition() {
		return edition;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Optional<String> getOptionalDatabaseName() {
		return Optional.ofNullable(databaseName);
	}

	@Override
	public Optional<String> getOptionalSchemaDatabaseName() {
		return Optional.ofNullable(schemaDatabaseName);
	}
}
