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
package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

/**
 * Contextual information passed to renderers.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
public final class RenderConfig {

	static RenderConfig defaultConfig() {
		return new RenderConfig(Neo4jVersion.UNDEFINED, Neo4jEdition.UNDEFINED, null, true);
	}

	/**
	 * Allows adding idempotency to the context.
	 */
	public interface IfNotExistsConfigBuilder extends Builder {

		/**
		 * @return a context that renders its statements in an idempotent fashion if possible
		 */
		default Builder ifNotExists() {
			return this.idempotent(true);
		}
	}

	/**
	 * Allows adding idempotency to the context.
	 */
	public interface IfExistsConfigBuilder extends Builder {

		/**
		 * @return a context that renders its statements in an idempotent fashion if possible
		 */
		default Builder ifExists() {
			return this.idempotent(true);
		}
	}

	/**
	 * Defines the version and the edition of the current context. They will be parsed in a lenient way.
	 */
	public interface Builder {

		/**
		 * @param version will be parsed lenient into a {@link Neo4jVersion} abd defazkt to {@link Neo4jVersion#LATEST}
		 * @param edition will be parsed lenient into a {@link Neo4jEdition} and default to {@link Neo4jEdition#UNDEFINED}
		 * @return a config accommodating the given version and edition
		 */
		default RenderConfig forVersionAndEdition(String version, String edition) {
			return forVersionAndEdition(Neo4jVersion.of(version), Neo4jEdition.of(edition));
		}

		/**
		 * @param version the target version of this config
		 * @param edition the target edition of this config
		 * @return a config accommodating the given version and edition
		 */
		RenderConfig forVersionAndEdition(Neo4jVersion version, Neo4jEdition edition);

		/**
		 * Turn the outcome into a potentially idempotent fashion
		 *
		 * @param idempotent set to {@literal true} to produce an idempotent config
		 * @return this builder
		 */
		Builder idempotent(boolean idempotent);
	}

	private static class DefaultBuilder implements IfNotExistsConfigBuilder, IfExistsConfigBuilder {

		private final Operator operator;

		private boolean idempotent;

		private DefaultBuilder(Operator operator) {
			this.operator = operator;
		}

		@Override
		public RenderConfig forVersionAndEdition(Neo4jVersion version, Neo4jEdition edition) {
			return new RenderConfig(version, edition, operator, idempotent);
		}

		@Override
		@SuppressWarnings("HiddenField")
		public Builder idempotent(boolean idempotent) {
			this.idempotent = idempotent;
			return this;
		}
	}

	/**
	 * Starts building a render context that eventually will result in a {@literal CREATE ...} statement.
	 * @return An ongoing build step
	 */
	public static IfNotExistsConfigBuilder create() {
		return new DefaultBuilder(Operator.CREATE);
	}

	/**
	 * Starts building a render context that eventually will result in a {@literal DROP ...} statement.
	 * @return An ongoing build step
	 */
	public static IfExistsConfigBuilder drop() {
		return new DefaultBuilder(Operator.DROP);
	}

	/**
	 * Neo4j version used to get any of the contained information.
	 */
	private final Neo4jVersion version;

	/**
	 * Neo4j edition used to get any of the contained information.
	 */
	private final Neo4jEdition edition;

	private final Operator operator;

	private final boolean idempotent;

	/**
	 * Flag if the name should be ignored.
	 */
	private final boolean ignoreName;

	RenderConfig(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent) {
		this(version, edition, operator, idempotent, false);
	}

	RenderConfig(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent,
		boolean ignoreName) {
		this.version = version;
		this.edition = edition;
		this.operator = operator;
		this.idempotent = idempotent;
		this.ignoreName = ignoreName;
	}

	Neo4jVersion getVersion() {
		return version;
	}

	Neo4jEdition getEdition() {
		return edition;
	}

	Operator getOperator() {
		return operator;
	}

	boolean isIdempotent() {
		return idempotent;
	}

	boolean isVersionPriorTo44() {
		return getVersion().isPriorTo44();
	}

	boolean isIgnoreName() {
		return ignoreName;
	}

	/**
	 * This is useful to get a render context that ignores the name of an object to force dropping things created without a name.
	 * @return a new context ignoring the name
	 */
	public RenderConfig ignoreName() {
		return new RenderConfig(version, edition, operator, idempotent, true);
	}

	/**
	 * @return the snippet necessary to use an idempotent operation for the given operation in this config.
	 */
	String ifNotExistsOrEmpty() {
		if (getOperator() == Operator.CREATE) {
			return isIdempotent() ? "IF NOT EXISTS " : "";
		} else if (getOperator() == Operator.DROP) {
			return isIdempotent() ? " IF EXISTS" : "";
		} else {
			throw new IllegalStateException();
		}
	}
}
