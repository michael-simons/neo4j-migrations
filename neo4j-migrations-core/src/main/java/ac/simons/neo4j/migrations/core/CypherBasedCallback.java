/*
 * Copyright 2020-2021 the original author or authors.
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

import java.net.URL;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.SessionConfig;

/**
 * A callback based on a Cypher script.
 *
 * @author Michael J. Simons
 * @since 1.2.2
 */
final class CypherBasedCallback implements Callback {

	private static final Logger LOGGER = Logger.getLogger(CypherBasedCallback.class.getName());

	private final CypherResource cypherResource;

	private final LifecyclePhase phase;

	private final String description;

	CypherBasedCallback(URL url, boolean autocrlf) {

		this.cypherResource = new CypherResource(url, autocrlf);

		String script = this.cypherResource.getScript();
		Matcher matcher = LifecyclePhase.LIFECYCLE_PATTERN.matcher(script);
		if (!matcher.matches()) {
			throw new MigrationsException("Invalid name for a callback script: " + script);
		}

		this.phase = LifecyclePhase.fromCamelCase(matcher.group(1));
		this.description = matcher.group(2);
	}

	@Override
	public LifecyclePhase getPhase() {
		return this.phase;
	}

	@Override
	public Optional<String> getOptionalDescription() {
		return Optional.ofNullable(this.description);
	}

	@Override
	public String getSource() {
		return this.cypherResource.getScript();
	}

	@Override
	public void on(LifecycleEvent event) {

		LOGGER.log(Level.FINE, "Invoking \"{0}\" on {1}",
			new Object[] { this.cypherResource.getUrl(), event.getPhase() });

		UnaryOperator<SessionConfig.Builder> sessionCustomizer = event.getPhase() == LifecyclePhase.BEFORE_FIRST_USE ?
			builder -> SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE) : UnaryOperator.identity();
		this.cypherResource.executeIn(event.getContext(), sessionCustomizer);
	}
}
