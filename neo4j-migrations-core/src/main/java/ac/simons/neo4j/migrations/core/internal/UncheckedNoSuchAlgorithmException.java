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
package ac.simons.neo4j.migrations.core.internal;

/**
 * Unchecked analogue to {@link java.io.UncheckedIOException}.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class UncheckedNoSuchAlgorithmException extends RuntimeException {

	private static final long serialVersionUID = 6879942930900663370L;

	UncheckedNoSuchAlgorithmException(Throwable cause) {
		super(cause);
	}

}
