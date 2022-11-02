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

import java.util.regex.Matcher;

/**
 * Options for a single {@link Migration}.
 * @author Michael J. Simons
 * @since TBA
 */
public final class MigrationOptions {

	private static final MigrationOptions DEFAULT_INSTANCE = new MigrationOptions(false, false);

	private final boolean runAlways;

	private final boolean runOnChange;

	static MigrationOptions fromPathOrUrl(String pathOrUrl) {

		Matcher matcher = MigrationVersion.VERSION_PATTERN.matcher(pathOrUrl);
		if (!matcher.find()) {
			return DEFAULT_INSTANCE;
		}
		String options = matcher.group("options");
		if (options == null || options.trim().isEmpty()) {
			return DEFAULT_INSTANCE;
		}

		boolean runAlways = options.equalsIgnoreCase("runAlways");
		boolean runOnChange = options.equalsIgnoreCase("runOnChange");
		return new MigrationOptions(runAlways, runOnChange);
	}

	public static MigrationOptions defaults() {
		return DEFAULT_INSTANCE;
	}

	public static MigrationOptions runAlways() {
		return new MigrationOptions(true, false);
	}

	public static MigrationOptions runOnChange() {
		return new MigrationOptions(false, true);
	}

	MigrationOptions(boolean runAlways, boolean runOnChange) {
		this.runAlways = runAlways;
		this.runOnChange = runOnChange;
	}

	public boolean isRunAlways() {
		return runAlways;
	}

	public boolean isRunOnChange() {
		return runOnChange;
	}
}
