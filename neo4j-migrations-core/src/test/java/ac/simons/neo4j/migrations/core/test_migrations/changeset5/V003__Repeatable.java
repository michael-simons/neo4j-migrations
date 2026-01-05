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
package ac.simons.neo4j.migrations.core.test_migrations.changeset5;

import java.util.Optional;
import java.util.UUID;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
public class V003__Repeatable implements JavaBasedMigration {

	private boolean repeatable = true;

	private String checksum = UUID.randomUUID().toString();

	@Override
	public void apply(MigrationContext context) {

		try (Session session = context.getSession()) {
			session.run("CREATE (n:V003__Repeatable)").consume();
		}
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.of(this.checksum);
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	@Override
	public boolean isRepeatable() {
		return this.repeatable;
	}

	public void setRepeatable(boolean repeatable) {
		this.repeatable = repeatable;
	}

}
