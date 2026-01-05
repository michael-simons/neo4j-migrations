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

import java.util.concurrent.TimeUnit;

/**
 * A simple stop watch.
 *
 * @author Michael J. Simons
 * @since 0.0.4
 */
final class StopWatch {

	/**
	 * The timestamp at which the stopwatch was started.
	 */
	private long start = -1L;

	/**
	 * The timestamp at which the stopwatch was stopped.
	 */
	private long stop = -1L;

	/**
	 * Starts the stop watch.
	 */
	void start() {
		if (this.start != -1L) {
			throw new IllegalStateException("Stopwatch already started.");
		}
		this.start = nanoTime();
	}

	/**
	 * Stops the stop watch.
	 * @return the total run time in millis of the stop watch between the start and stop
	 * calls.
	 */
	long stop() {
		if (this.start == -1L) {
			throw new IllegalStateException("Stopwatch not started.");
		}

		if (this.stop != -1L) {
			throw new IllegalStateException("Stopwatch already stopped.");
		}
		this.stop = nanoTime();

		return getTotalTimeMillis();
	}

	/**
	 * Resets this stop watch.
	 */
	void reset() {
		this.start = -1L;
		this.stop = -1L;
	}

	private long nanoTime() {
		return System.nanoTime();
	}

	/**
	 * Returns the total run time in millis of the stop watch between start and stop
	 * calls.
	 * @return the total time this watch measured
	 */
	private long getTotalTimeMillis() {

		if (this.start == -1) {
			throw new IllegalStateException("Stopwatch not started.");
		}

		if (this.stop == -1) {
			throw new IllegalStateException("Stopwatch not stopped.");
		}

		long duration = this.stop - this.start;
		return TimeUnit.NANOSECONDS.toMillis(duration);
	}

}
