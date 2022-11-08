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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Only implementation of a {@link MigrationChain}.
 * @author Michael J. Simons
 * @since 2.0.0
 */
final class DefaultMigrationChain implements MigrationChain {

	private final ConnectionDetails connectionDetailsDelegate;

	private final Map<MigrationVersion, Element> elements;

	DefaultMigrationChain(ConnectionDetails connectionDetailsDelegate, Map<MigrationVersion, Element> elements) {
		this.connectionDetailsDelegate = connectionDetailsDelegate;
		this.elements = elements;
	}

	@Override
	public String getServerAddress() {
		return connectionDetailsDelegate.getServerAddress();
	}

	@Override
	public String getServerVersion() {
		return connectionDetailsDelegate.getServerVersion();
	}

	@Override
	public String getServerEdition() {
		return connectionDetailsDelegate.getServerEdition();
	}

	@Override
	public String getUsername() {
		return connectionDetailsDelegate.getUsername();
	}

	@Override
	public Optional<String> getOptionalDatabaseName() {
		return connectionDetailsDelegate.getOptionalDatabaseName();
	}

	@Override
	public Optional<String> getOptionalSchemaDatabaseName() {
		return connectionDetailsDelegate.getOptionalSchemaDatabaseName();
	}

	@Override
	public boolean isApplied(String version) {
		Element element = this.elements.get(MigrationVersion.withValue(version));
		return element != null && element.getState() == MigrationState.APPLIED;
	}

	@Override
	public Collection<Element> getElements() {
		return this.elements.values();
	}

	@Override
	public Optional<MigrationVersion> getLastAppliedVersion() {
		Iterator<MigrationVersion> it = this.elements.keySet().iterator();
		MigrationVersion version = null;
		while (it.hasNext()) {
			version = it.next();
		}
		return Optional.ofNullable(version);
	}
}
