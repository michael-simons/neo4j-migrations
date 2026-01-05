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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The result of the {@link Migrations#validate} operation.
 *
 * @author Michael J. Simons
 * @since 1.2.0
 */
public final class ValidationResult implements DatabaseOperationResult {

	private static final Set<Outcome> NEEDS_REPAIR = EnumSet.of(Outcome.INCOMPLETE_MIGRATIONS,
			Outcome.DIFFERENT_CONTENT, Outcome.UNDEFINED);

	private final String affectedDatabase;

	private final Outcome outcome;

	private final List<String> warnings;

	ValidationResult(Optional<String> affectedDatabase, Outcome outcome, List<String> warnings) {

		this.affectedDatabase = affectedDatabase.orElse(null);
		this.outcome = outcome;
		this.warnings = new ArrayList<>(warnings);
	}

	@Override
	public Collection<String> getWarnings() {
		return Collections.unmodifiableList(this.warnings);
	}

	@Override
	public String prettyPrint() {
		String key = "validation-result.outcome." + this.outcome.name().toLowerCase(Locale.ROOT);
		return MessageFormat.format(Messages.INSTANCE.get(key),
				this.getAffectedDatabase().map(v -> "`" + v + "`").orElse("the default database"));
	}

	@Override
	public Optional<String> getAffectedDatabase() {
		return Optional.ofNullable(this.affectedDatabase);
	}

	/**
	 * Returns the outcome of the validation.
	 * @return the outcome of the validation
	 * @see #isValid
	 */
	public Outcome getOutcome() {
		return this.outcome;
	}

	/**
	 * {@return <code>true</code> if the database is in a valid state}
	 */
	public boolean isValid() {
		return this.outcome == Outcome.VALID;
	}

	/**
	 * If this method returns {@literal false}, the database is either valid or the
	 * resolved migrations can be applied to bring the database into a valid state.
	 * @return {@literal true} if the database needs repair.
	 */
	public boolean needsRepair() {
		return NEEDS_REPAIR.contains(this.outcome);
	}

	/**
	 * While {@link ValidationResult#isValid()} returns whether a database is in a valid
	 * state regarding migrations, the outcome gives the actual details.
	 *
	 */
	public enum Outcome {

		/**
		 * The database is in an undefined state.
		 */
		UNDEFINED,

		/**
		 * The combined state of migrations resolved and applied to the target database is
		 * valid.
		 */
		VALID,

		/**
		 * The target database has not been fully migrated, meaning there are resolved
		 * migrations that have not been applied. The database can be brought into a valid
		 * state via {@link Migrations#validate()}.
		 */
		INCOMPLETE_DATABASE,

		/**
		 * Some migrations have been applied to the target database that cannot be
		 * resolved locally any longer. The database must be repaired.
		 */
		INCOMPLETE_MIGRATIONS,

		/**
		 * Some migrations have been changed since they have been applied to the target
		 * database or their version doesn't fit anymore. The database must be repaired.
		 */
		DIFFERENT_CONTENT

	}

}
