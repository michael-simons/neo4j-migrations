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
 * Default implementation to contain {@link LifecycleEvent lifecycle events}.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
@SuppressWarnings({ "squid:S6206", "ClassCanBeRecord" }) // No, but thanks
final class DefaultLifecycleEvent implements LifecycleEvent {

	private final LifecyclePhase phase;

	private final MigrationContext context;

	DefaultLifecycleEvent(LifecyclePhase phase, MigrationContext context) {
		this.phase = phase;
		this.context = context;
	}

	@Override
	public LifecyclePhase getPhase() {
		return this.phase;
	}

	@Override
	public MigrationContext getContext() {
		return this.context;
	}

}
