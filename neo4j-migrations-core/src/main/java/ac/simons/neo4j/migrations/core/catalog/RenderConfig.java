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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;

/**
 * Contextual information passed to renderers.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class RenderConfig {

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

	@SuppressWarnings("squid:S1874")
	private final List<AdditionalRenderingOptions> additionalOptions;

	/**
	 * Flag to include options when rendering to cypher.
	 */
	private final boolean includingOptions;

	/**
	 * Flag to include an explicit index type for property indizes when available.
	 */
	private final boolean useExplicitPropertyIndexType;

	RenderConfig(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent) {
		this(version, edition, operator, idempotent, false, null);
	}

	RenderConfig(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent, boolean ignoreName,
			@SuppressWarnings("squid:S1874") List<AdditionalRenderingOptions> additionalOptions) {
		this.version = version;
		this.edition = edition;
		this.operator = operator;
		this.idempotent = idempotent;
		this.ignoreName = ignoreName;
		this.additionalOptions = (additionalOptions != null) ? additionalOptions : Collections.emptyList();

		List<CypherRenderingOptions> cypherRenderingOptions = this.additionalOptions.stream()
			.filter(CypherRenderingOptions.class::isInstance)
			.map(CypherRenderingOptions.class::cast)
			.toList();

		this.includingOptions = cypherRenderingOptions.stream()
			.map(CypherRenderingOptions::includingOptions)
			.reduce(!cypherRenderingOptions.isEmpty(), (v1, v2) -> v1 && v2);
		this.useExplicitPropertyIndexType = cypherRenderingOptions.stream()
			.map(CypherRenderingOptions::useExplicitPropertyIndexType)
			.reduce(!cypherRenderingOptions.isEmpty(), (v1, v2) -> v1 && v2);
	}

	static RenderConfig defaultConfig() {
		return new RenderConfig(Neo4jVersion.UNDEFINED, Neo4jEdition.UNDEFINED, null, true);
	}

	/**
	 * Starts building a render context that eventually will result in a
	 * {@literal CREATE ...} statement.
	 * @return an ongoing build step
	 */
	public static IfNotExistsConfigBuilder create() {
		return new DefaultBuilder(Operator.CREATE);
	}

	/**
	 * Starts building a render context that eventually will result in a
	 * {@literal DROP ...} statement.
	 * @return an ongoing build step
	 */
	public static IfExistsConfigBuilder drop() {
		return new DefaultBuilder(Operator.DROP);
	}

	Neo4jVersion getVersion() {
		return this.version;
	}

	Neo4jEdition getEdition() {
		return this.edition;
	}

	Operator getOperator() {
		return this.operator;
	}

	boolean isIdempotent() {
		return this.idempotent;
	}

	boolean isVersionPriorTo44() {
		return getVersion().isPriorTo44();
	}

	boolean isIgnoreName() {
		return this.ignoreName;
	}

	@SuppressWarnings("squid:S1874")
	List<AdditionalRenderingOptions> getAdditionalOptions() {
		return Collections.unmodifiableList(this.additionalOptions);
	}

	boolean includeOptions() {
		return this.includingOptions;
	}

	boolean useExplicitPropertyIndexType() {
		return this.useExplicitPropertyIndexType;
	}

	/**
	 * This is useful to get a render context that ignores the name of an object to force
	 * dropping things created without a name.
	 * @return a new context ignoring the name
	 */
	public RenderConfig ignoreName() {
		return new RenderConfig(this.version, this.edition, this.operator, this.idempotent, true,
				this.additionalOptions);
	}

	/**
	 * Adds additional options to the renderer or deletes the existing ones.
	 * @param newOptions new list of options, may be {@literal null} or empty
	 * @return a (potentially) new {@link RenderConfig}
	 * @since 1.11.0
	 */
	public RenderConfig withAdditionalOptions(
			@SuppressWarnings("squid:S1874") List<? extends AdditionalRenderingOptions> newOptions) {
		if (Objects.equals(this.additionalOptions, newOptions)) {
			return this;
		}

		return new RenderConfig(this.version, this.edition, this.operator, this.idempotent, this.ignoreName,
				(newOptions != null) ? List.copyOf(newOptions) : null);
	}

	/**
	 * Returns the snippet necessary to use an idempotent operation for the given
	 * operation in this config.
	 * @return a snippet makes a cypher statement idempotent for this configuration
	 */
	String ifNotExistsOrEmpty() {
		if (getOperator() == Operator.CREATE) {
			return isIdempotent() ? "IF NOT EXISTS " : "";
		}
		else if (getOperator() == Operator.DROP) {
			return isIdempotent() ? " IF EXISTS" : "";
		}
		else {
			throw new IllegalStateException();
		}
	}

	/**
	 * Additional options passed to a {@link RenderConfig configuration}.
	 * <strong>Warning</strong>: Not to be implemented by user code. Will be sealed in a
	 * later release.
	 *
	 * @since 1.11.0
	 */
	public sealed interface AdditionalRenderingOptions permits XMLRenderingOptions, CypherRenderingOptions {

	}

	/**
	 * Additional options passed to an XML renderer. Some options might be ignored for
	 * some content.
	 *
	 * @since 1.11.0
	 */
	public non-sealed interface XMLRenderingOptions extends AdditionalRenderingOptions {

		/**
		 * Only applicable to {@link Catalog catalogs}.
		 * @return <code>true</code> to add an {@code <apply />} element.
		 */
		default boolean withApply() {
			return false;
		}

		/**
		 * Only applicable to {@link Catalog catalogs}.
		 * @return <code>true</code> to add an {@code reset} attribute.
		 */
		default boolean withReset() {
			return false;
		}

		/**
		 * {@return optional comment to add to the generated document}
		 */
		default Optional<String> optionalHeader() {
			return Optional.empty();
		}

	}

	/**
	 * Additional options passed to a Cypher renderer. Some options might be ignored for
	 * some content.
	 *
	 * @since 1.13.0
	 */
	public non-sealed interface CypherRenderingOptions extends AdditionalRenderingOptions {

		/**
		 * {@return <code>true</code> to enable rendering of options}
		 */
		default boolean includingOptions() {
			return false;
		}

		/**
		 * This setting will only have effect if an index uses an option map defining a
		 * valid index provider for property indexes.
		 * @return <code>true</code> to enable the use of explicit index types for
		 * property index based on the index provider
		 */
		default boolean useExplicitPropertyIndexType() {
			return false;
		}

	}

	/**
	 * Allows adding idempotency to the context.
	 */
	public interface IfNotExistsConfigBuilder extends Builder {

		/**
		 * Makes the builder create idempotent cypher statements.
		 * @return this builder
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
		 * Makes the builder create idempotent cypher statements.
		 * @return this builder
		 */
		default Builder ifExists() {
			return this.idempotent(true);
		}

	}

	/**
	 * Defines the version and the edition of the current context. They will be parsed in
	 * a lenient way.
	 */
	public interface Builder {

		/**
		 * Creates a new configuration that renders according the given version and
		 * edition.
		 * @param version will be parsed lenient into a static version and default to
		 * {@code LATEST}
		 * @param edition will be parsed lenient into a static edition and default to
		 * {@code UNDEFINED}
		 * @return a config accommodating the given version and edition
		 */
		default RenderConfig forVersionAndEdition(String version, String edition) {
			return forVersionAndEdition(Neo4jVersion.of(version), Neo4jEdition.of(edition));
		}

		/**
		 * Creates a new configuration that renders according the given version and
		 * edition.
		 * @param version the target version of this config
		 * @param edition the target edition of this config
		 * @return a config accommodating the given version and edition
		 */
		RenderConfig forVersionAndEdition(Neo4jVersion version, Neo4jEdition edition);

		/**
		 * Turn the outcome into a potentially idempotent fashion.
		 * @param idempotent set to <code>true</code> to produce an idempotent config
		 * @return this builder
		 */
		Builder idempotent(boolean idempotent);

	}

	private static final class DefaultBuilder implements IfNotExistsConfigBuilder, IfExistsConfigBuilder {

		private final Operator operator;

		private boolean idempotent;

		private DefaultBuilder(Operator operator) {
			this.operator = operator;
		}

		@Override
		public RenderConfig forVersionAndEdition(Neo4jVersion version, Neo4jEdition edition) {
			return new RenderConfig(version, edition, this.operator, this.idempotent);
		}

		@Override
		@SuppressWarnings("HiddenField")
		public Builder idempotent(boolean idempotent) {
			this.idempotent = idempotent;
			return this;
		}

	}

}
