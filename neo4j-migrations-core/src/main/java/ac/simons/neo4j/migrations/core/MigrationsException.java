/*
 * Copyright 2020-2024 the original author or authors.
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
 * An unchecked exception that is thrown when something didn't work as expected. Most of the time, mitigation from the
 * call won't be possible.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public final class MigrationsException extends RuntimeException {

	static final long serialVersionUID = 1L;

	/**
	 * Constructs a new exception with the given message.
	 *
	 * @param message the detail message
	 */
	public MigrationsException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the given message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public MigrationsException(String message, Throwable cause) {
		super(message, cause);
	}
}
