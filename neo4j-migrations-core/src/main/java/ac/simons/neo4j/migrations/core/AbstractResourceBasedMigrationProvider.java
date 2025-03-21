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

/**
 * A utility class to avoid duplication of common characteristics of {@link ResourceBasedMigrationProvider migration providers}.
 *
 * @author Michael J. Simons
 * @since 1.9.0
 */
public abstract class AbstractResourceBasedMigrationProvider implements ResourceBasedMigrationProvider {

	private final int order;

	private final String extension;

	private final boolean supportsArbitraryResourceNames;

	/**
	 * The given parameters are all required.
	 *
	 * @param order                          The order in which this provider should be consulted
	 * @param extension                      The file extension that this provider can deal with
	 * @param supportsArbitraryResourceNames A flag if arbitrary resource names are supported in contrast to names that match {@link MigrationVersion#VERSION_PATTERN}
	 */
	protected AbstractResourceBasedMigrationProvider(int order, String extension,
		boolean supportsArbitraryResourceNames) {
		this.order = order;
		this.extension = extension;
		this.supportsArbitraryResourceNames = supportsArbitraryResourceNames;
	}

	@Override
	public final int getOrder() {
		return order;
	}

	@Override
	public final String getExtension() {
		return extension;
	}

	@Override
	public final boolean supportsArbitraryResourceNames() {
		return supportsArbitraryResourceNames;
	}
}
