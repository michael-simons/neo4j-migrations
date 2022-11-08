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
package ac.simons.neo4j.migrations.maven;

import ac.simons.neo4j.migrations.core.Messages;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.ValidationResult;

import java.util.Collection;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Calling {@link Migrations#validate()} during verify phase.
 *
 * @author Michael J. Simons
 * @soundtrack Kid Rock - Devil Without A Cause
 * @since 1.2.0
 */
@Mojo(name = "validate",
	requiresDependencyResolution = ResolutionScope.TEST,
	defaultPhase = LifecyclePhase.VERIFY,
	threadSafe = true)
public class ValidateMojo extends AbstractConnectedMojo {

	/**
	 * Set this to {@literal false} if the database can be brought into a valid state by just applying the configuration
	 */
	@Parameter(defaultValue = "true")
	private boolean alwaysFail;

	@Override
	void withMigrations(Migrations migrations) throws MojoFailureException {

		ValidationResult validationResult = migrations.validate();
		if (validationResult.isValid()) {
			LOGGER.info(validationResult::prettyPrint);
		} else {
			StringBuilder message = new StringBuilder(validationResult.prettyPrint());
			Collection<String> warnings = validationResult.getWarnings();
			if (!warnings.isEmpty()) {
				message
					.append(System.lineSeparator())
					.append(String.join(System.lineSeparator(), warnings));
			}

			boolean needsRepair = validationResult.needsRepair();
			String shortMessage = Messages.INSTANCE.get(needsRepair ? "validation.database_needs_repair" : "validation.database_is_invalid");
			if (alwaysFail || needsRepair) {
				throw new MojoFailureException(validationResult, shortMessage, message.toString());
			} else {
				LOGGER.warning(shortMessage);
				LOGGER.warning(message::toString);
			}
		}
	}
}
