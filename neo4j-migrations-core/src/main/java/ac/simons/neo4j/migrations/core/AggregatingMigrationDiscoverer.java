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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregating the results of a collection of discoverer into one big result.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class AggregatingMigrationDiscoverer implements Discoverer<Migration> {

	private final Collection<Discoverer<Migration>> delegates;

	AggregatingMigrationDiscoverer(Collection<Discoverer<Migration>> delegates) {
		this.delegates = delegates;
	}

	@Override
	public Collection<Migration> discover(MigrationContext context) {
		List<Migration> result = new ArrayList<>();
		for (Discoverer<Migration> discoverer : this.delegates) {
			result.addAll(discoverer.discover(context));
		}
		return result;
	}

}
