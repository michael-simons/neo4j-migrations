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

import java.util.Locale;

/**
 * @since 1.4.1
 */
final class CypherPrecondition extends AbstractPrecondition implements Precondition {

	private final String targetDatabase;

	private final String query;

	CypherPrecondition(Type type, String query, String targetDatabase) {
		super(type);
		this.query = query;
		this.targetDatabase = targetDatabase == null ? "target" : targetDatabase.toLowerCase(Locale.ROOT);
	}

	@Override
	public boolean isSatisfied(MigrationContext migrationContext) {
		if ("schema".equals(targetDatabase)) {
			return migrationContext.getSchemaSession().run(query).single().get(0).asBoolean();
		} else if ("target".equals(targetDatabase)) {
			return migrationContext.getSession().run(query).single().get(0).asBoolean();
		} else {
			throw new IllegalStateException("Target database not supported. Available databases are 'schmema' or 'target'.");
		}
	}
}
