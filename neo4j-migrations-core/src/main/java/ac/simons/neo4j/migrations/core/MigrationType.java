/*
 * Copyright 2020 the original author or authors.
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

/**
 * Type of a migration.
 *
 * @author Michael J. Simons
 */
public final class MigrationType {

	/**
	 * The reason not making the type a top level enum is simple: I don't want it to be used outside our package.
	 */
	enum Value {
		JAVA, CYPHER;
	}

	/**
	 * Indicates a Java based migration.
	 */
	static final MigrationType JAVA = new MigrationType(Value.JAVA);
	/**
	 * Indicates a Cypher based migration.
	 */
	static final MigrationType CYPHER = new MigrationType(Value.CYPHER);

	private final Value value;

	private MigrationType(final Value value) {
		this.value = value;
	}

	Value getValue() {
		return value;
	}
}
