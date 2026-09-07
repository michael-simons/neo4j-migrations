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

import java.time.Duration;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import ac.simons.neo4j.migrations.core.MigrationVersion.StopVersion;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable MigrationVersion optionalStop;

	private IterableMigrations(MigrationsConfig config, List<Migration> migrations,
			@Nullable MigrationVersion optionalStop) {
		this.config = config;
		this.migrations = migrations;
		this.optionalStop = optionalStop;
	}

	static IterableMigrations of(MigrationsConfig config, List<Migration> migrations) {
		return of(config, migrations, null);
	}

	static IterableMigrations of(MigrationsConfig config, List<Migration> migrations,
			@Nullable StopVersion stopVersion) {
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

		private final @Nullable Duration optionalDelay;

		private final Comparator<MigrationVersion> comparator;

		private final @Nullable MigrationVersion optionalStop;

		private @Nullable Migration next;

		private boolean afterFirst;

		DelayingIterator(Iterator<Migration> delegate, @Nullable Duration optionalDelay,
				Comparator<MigrationVersion> comparator, @Nullable MigrationVersion optionalStop) {
			this.delegate = delegate;
			this.optionalDelay = optionalDelay;
			this.comparator = comparator;
			this.optionalStop = optionalStop;
			this.next = advance();
		}

		private @Nullable Migration advance() {
			if (!this.delegate.hasNext()) {
				return null;
			}
			var candidate = this.delegate.next();
			return (this.optionalStop == null
					|| this.comparator.compare(candidate.getVersion(), this.optionalStop) <= 0) ? candidate : null;
		}

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@Override
		public Migration next() throws NoSuchElementException {
			var current = this.next;
			if (current == null) {
				throw new NoSuchElementException();
			}
			this.next = advance();
			if (this.optionalDelay != null && this.afterFirst) {
				try {
					Thread.sleep(this.optionalDelay.toMillis());
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			this.afterFirst = true;
			return current;
		}

	}

}
