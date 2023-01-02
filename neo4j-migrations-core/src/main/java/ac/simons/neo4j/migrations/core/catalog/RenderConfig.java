/*
 * Copyright 2020-2023 the original author or authors.
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

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contextual information passed to renderers.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since 1.7.0
 */
public final class RenderConfig {

	static RenderConfig defaultConfig() {
		return new RenderConfig(Neo4jVersion.UNDEFINED, Neo4jEdition.UNDEFINED, null, true);
	}

	/**
	 * Additional options passed to a {@link RenderConfig configuration}.
	 * <strong>Warning</strong>: Not to be implemented by user code. Will be sealed in a later release.
	 *
	 * @since 1.11.0
	 * @deprecated Not meant to be part of a public API, but cannot restrict it in Java 8
	 */
	@Deprecated
	@SuppressWarnings({"DeprecatedIsStillUsed", "squid:S1133"})
	public interface AdditionalRenderingOptions {
	}

	/**
	 * Additional options passed to an XML renderer. Some options might be ignored for some content.
	 *
	 * @since 1.11.0
	 */
	@SuppressWarnings("squid:S1874") // Complains about the purposeful deprecated option
	public interface XMLRenderingOptions extends AdditionalRenderingOptions {

		/**
		 * Only applicable to {@link Catalog catalogs}.
		 *
		 * @return {@literal true} to add an {@code <apply />} element.
		 */
		default boolean withApply() {
			return false;
		}

		/**
		 * Only applicable to {@link Catalog catalogs}.
		 *
		 * @return {@literal true} to add an {@code reset} attribute.
		 */
		default boolean withReset() {
			return false;
		}

		/**
		 * @return Optional comment to add to the generated document
		 */
		default Optional<String> optionalHeader() {
			return Optional.empty();
		}
	}

	/**
	 * Additional options passed to a Cypher renderer. Some options might be ignored for some content.
	 *
	 * @since 1.13.0
	 */
	@SuppressWarnings("squid:S1874") // Complains about the purposeful deprecated option
	public interface CypherRenderingOptions extends AdditionalRenderingOptions {

		/**
		 * @return {@literal true} to enable rendering of options
		 */
		default boolean includingOptions() {
			return false;
		}

		/**
		 * This setting will only have effect if an index uses an option map defining a valid index provider for property indexes.
		 *
		 * @return {@literal true} to enable the use of explicit index types for property index based on the index provider
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
		 * @param version will be parsed lenient into a static version and default to {@code LATEST}
		 * @param edition will be parsed lenient into a static edition and default to {@code UNDEFINED}
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

	RenderConfig(Neo4jVersion version, Neo4jEdition edition, Operator operator, boolean idempotent,
		boolean ignoreName, @SuppressWarnings("squid:S1874") List<AdditionalRenderingOptions> additionalOptions) {
		this.version = version;
		this.edition = edition;
		this.operator = operator;
		this.idempotent = idempotent;
		this.ignoreName = ignoreName;
		this.additionalOptions = additionalOptions == null ? Collections.emptyList() : additionalOptions;

		List<CypherRenderingOptions> cypherRenderingOptions = this.additionalOptions.stream()
			.filter(CypherRenderingOptions.class::isInstance)
			.map(CypherRenderingOptions.class::cast)
			.collect(Collectors.toList());

		this.includingOptions = cypherRenderingOptions.stream()
			.map(CypherRenderingOptions::includingOptions)
			.reduce(!cypherRenderingOptions.isEmpty(), (v1, v2) -> v1 && v2);
		this.useExplicitPropertyIndexType = cypherRenderingOptions.stream()
			.map(CypherRenderingOptions::useExplicitPropertyIndexType)
			.reduce(!cypherRenderingOptions.isEmpty(), (v1, v2) -> v1 && v2);
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

	@SuppressWarnings("squid:S1874")
	List<AdditionalRenderingOptions> getAdditionalOptions() {
		return Collections.unmodifiableList(additionalOptions);
	}

	boolean includeOptions() {
		return includingOptions;
	}

	boolean useExplicitPropertyIndexType() {
		return useExplicitPropertyIndexType;
	}

	/**
	 * This is useful to get a render context that ignores the name of an object to force dropping things created without a name.
	 * @return a new context ignoring the name
	 */
	public RenderConfig ignoreName() {
		return new RenderConfig(version, edition, operator, idempotent, true, additionalOptions);
	}

	/**
	 * Adds additional options to the renderer or deletes the existing ones.
	 *
	 * @param newOptions New list of options, may be {@literal null} or empty
	 * @return A (potentially) new {@link RenderConfig}
	 * @since 1.11.0
	 */
	public RenderConfig withAdditionalOptions(@SuppressWarnings("squid:S1874") List<? extends AdditionalRenderingOptions> newOptions) {
		if (Objects.equals(this.additionalOptions, newOptions)) {
			return this;
		}

		return new RenderConfig(version, edition, operator, idempotent, ignoreName, newOptions == null ? null : new ArrayList<>(newOptions));
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
