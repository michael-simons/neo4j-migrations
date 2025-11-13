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

import java.time.Duration;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.MigrationVersion.StopVersion;

/**
 * A helper class that can be used to delay the iteration of migrations by a configurable
 * amount of time.
 *
 * @author Michael J. Simons
 * @since 2.3.2
 */
final class IterableMigrations implements Iterable<Migration> {

	private final MigrationsConfig config;

	private final List<Migration> migrations;

	private final MigrationVersion optionalStop;

	private IterableMigrations(MigrationsConfig config, List<Migration> migrations, MigrationVersion optionalStop) {
		this.config = config;
		this.migrations = migrations;
		this.optionalStop = optionalStop;
	}

	static IterableMigrations of(MigrationsConfig config, List<Migration> migrations) {
		return of(config, migrations, null);
	}

	static IterableMigrations of(MigrationsConfig config, List<Migration> migrations, StopVersion stopVersion) {
		MigrationVersion optionalStop;
		if (stopVersion == null) {
			optionalStop = null;
		}
		else {
			optionalStop = stopVersion.version();
			if (!stopVersion.optional()
					&& migrations.stream().filter(m -> m.getVersion().equals(optionalStop)).findFirst().isEmpty()) {
				throw new MigrationsException("Target version %s is not available".formatted(optionalStop.getValue()));
			}
		}
		return new IterableMigrations(config, migrations, optionalStop);
	}

	@Override
	public Iterator<Migration> iterator() {
		var iterator = this.migrations.iterator();
		if (this.optionalStop != null) {
			Migrations.LOGGER.log(Level.INFO, "Will stop at target version {0}", this.optionalStop);
		}
		return new DelayingIterator(iterator, this.config.getOptionalDelayBetweenMigrations().orElse(null),
				this.config.getVersionComparator(), this.optionalStop);
	}

	private static final class DelayingIterator implements Iterator<Migration> {

		private final Iterator<Migration> delegate;

		private final Duration optionalDelay;

		private final Comparator<MigrationVersion> comparator;

		private final MigrationVersion optionalStop;

		private Migration next;

		DelayingIterator(Iterator<Migration> delegate, Duration optionalDelay, Comparator<MigrationVersion> comparator,
				MigrationVersion optionalStop) {
			this.delegate = delegate;
			this.optionalDelay = optionalDelay;
			this.comparator = comparator;
			this.optionalStop = optionalStop;
		}

		@Override
		public boolean hasNext() {
			var hasNext = this.delegate.hasNext();
			if (hasNext) {
				this.next = this.delegate.next();
				hasNext = this.optionalStop == null
						|| this.comparator.compare(this.next.getVersion(), this.optionalStop) <= 0;
			}
			else {
				this.next = null;
			}
			return hasNext;
		}

		@Override
		public Migration next() throws NoSuchElementException {
			if (this.next == null) {
				throw new NoSuchElementException();
			}

			if (this.optionalDelay != null) {
				try {
					Thread.sleep(this.optionalDelay.toMillis());
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			return this.next;
		}

	}

}
