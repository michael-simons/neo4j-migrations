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
package ac.simons.neo4j.migrations.cli.internal;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Always log to stdout. Not for external use outside this program.
 *
 * @author Michael J. Simons
 * @since 0.0.5
 */
public final class MigrationsCliConsoleHandler extends StreamHandler {

	/**
	 * Creates a new handler with a fixed pointer to {@link System#out}.
	 */
	@SuppressWarnings("squid:S106") // Using System.out is the point of this exercise.
	public MigrationsCliConsoleHandler() {
		setOutputStream(System.out);
	}

	@Override
	public synchronized void publish(LogRecord logRecord) {
		super.publish(logRecord);
		flush();
	}

	@Override
	public synchronized void close() {
		flush();
	}

}
