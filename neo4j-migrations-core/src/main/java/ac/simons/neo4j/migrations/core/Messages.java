/*
 * Copyright 2020-2022 the original author or authors.
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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Accessor to messages shared between the core module and extensions.
 *
 * @author Michael J. Simons
 * @soundtrack Kid Rock - Devil Without A Cause
 * @since 1.2.0
 */
public enum Messages {

	/**
	 * The single instance.
	 */
	INSTANCE;

	private final ResourceBundle defaultBundle = ResourceBundle.getBundle(
		"ac.simons.neo4j.migrations.core.messages");

	/**
	 * Retrieves the message with the given key.
	 *
	 * @param key The key in the bundle
	 * @return A messsage
	 */
	public String get(String key) {
		return defaultBundle.getString(key);
	}

	/**
	 * Creates a {@link MessageFormat message format} from the value of the given key and uses the arguments to format it.
	 *
	 * @param key  The key of the message
	 * @param args The arguments to the format
	 * @return A formatted message
	 * @since 1.11.1
	 */
	public String format(String key, Object... args) {
		return MessageFormat.format(defaultBundle.getString(key), args);
	}
}
