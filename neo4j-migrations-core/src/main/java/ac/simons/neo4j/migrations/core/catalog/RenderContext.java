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
public final class RenderContext {

	static RenderContext defaultContext() {
		return new RenderContext(Neo4jVersion.UNDEFINED, Neo4jEdition.UNDEFINED, null, true);
	}

	/**
	 * Allows adding idempotency to the context.
	 */
	public interface IfNotExistsRenderContextBuilder extends RenderContextBuilder {

		/**
		 * @return a context that renders its statements in an idempotent fashion if possible
		 */
		RenderContextBuilder ifNotExists();
	}

	/**
	 * Allows adding idempotency to the context.
	 */
	public interface IfExistsRenderContextBuilder extends RenderContextBuilder {

		/**
		 * @return a context that renders its statements in an idempotent fashion if possible
		 */
		RenderContextBuilder ifExists();
	}

	/**
	 * Defines the version and the edition of the current context. They will be parsed in a lenient way.
	 */
	public interface RenderContextBuilder {

		/**
		 * @param version will be parsed lenient into a {@link Neo4jVersion} abd defazkt to {@link Neo4jVersion#LATEST}
		 * @param edition will be parsed lenient into a {@link Neo4jEdition} and default to {@link Neo4jEdition#UNDEFINED}
		 * @return a context accomodating the given version and context
		 */
		RenderContext forVersionAndEdition(String version, String edition);
	}

	private static class DefaultRenderContextBuilder implements IfNotExistsRenderContextBuilder, IfExistsRenderContextBuilder {

		private final Operator operator;

		private boolean idempotent;

		private DefaultRenderContextBuilder(Operator operator) {
			this.operator = operator;
		}

		@Override
		public RenderContext forVersionAndEdition(String version, String edition) {
			return new RenderContext(Neo4jVersion.of(version), Neo4jEdition.of(edition), operator, idempotent);
		}

		@Override
		public RenderContext.RenderContextBuilder ifNotExists() {
			this.idempotent = true;
			return this;
		}

		@Override
		public RenderContextBuilder ifExists() {
			this.idempotent = true;
			return this;
		}
	}

	/**
	 * Starts building a render context that eventually will result in a {@literal CREATE ...} statement.
	 * @return An ongoing build step
	 */
	public static IfNotExistsRenderContextBuilder create() {
		return new DefaultRenderContextBuilder(Operator.CREATE);
	}

	/**
	 * Starts building a render context that eventually will result in a {@literal DROP ...} statement.
	 * @return An ongoing build step
	 */
	public static IfExistsRenderContextBuilder drop() {
		return new DefaultRenderContextBuilder(Operator.DROP);
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

	RenderContext(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent) {
		this(version, edition, operator, idempotent, false);
	}

	RenderContext(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent,
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
	public RenderContext ignoreName() {
		return new RenderContext(version, edition, operator, idempotent, true);
	}
}
