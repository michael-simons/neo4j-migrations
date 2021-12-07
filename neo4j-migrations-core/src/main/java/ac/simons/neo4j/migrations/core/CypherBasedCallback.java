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
package ac.simons.neo4j.migrations.core;

import java.net.URL;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * A callback based on a Cypher script.
 *
 * @author Michael J. Simons
 * @since TBA
 */
final class CypherBasedCallback implements Callback {

	CypherBasedCallback(URL url) {
		this(url, Defaults.AUTOCRLF);
	}

	private final CypherResource cypherResource;

	CypherBasedCallback(URL url, boolean autocrlf) {

		this.cypherResource = new CypherResource(url, autocrlf);
//		super(url, autocrlf);
	//	this.version = MigrationVersion.parse(this.script);
	//	this.description = this.version.getDescription();
	}

	@Override
	public LifecyclePhase getPhase() {
		return null;
	}

	@Override
	public void invoke(MigrationContext context) throws MigrationsException {

		// Only in pre migrate, with default database
		try (Session session = context.getDriver().session(SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
			this.cypherResource.executeIn(session, context.getConfig().getTransactionMode());
		}
	}
}
