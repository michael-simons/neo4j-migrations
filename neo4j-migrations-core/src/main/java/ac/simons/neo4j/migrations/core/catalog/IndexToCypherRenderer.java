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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Formattable;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.Neo4jVersion;

/**
 * Renders indexes (supported operators are {@link Operator#CREATE} and
 * {@link Operator#DROP}) as Cypher.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.7.0
 */
@SuppressWarnings("squid:S6548")
enum IndexToCypherRenderer implements Renderer<Index> {

	INSTANCE;

	/**
	 * A range of versions from 3.5 to 4.2.
	 */
	private static final Set<Neo4jVersion> RANGE_35_TO_42 = EnumSet.of(Neo4jVersion.V3_5, Neo4jVersion.V4_0,
			Neo4jVersion.V4_1, Neo4jVersion.V4_2);

	private static final String ESCAPED_UNICODE_QUOTE = "\\u0027";

	private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)'");

	private static final UnaryOperator<String> TO_LITERAL = v -> {
		String result = UNESCAPED_QUOTE.matcher(v.replace(ESCAPED_UNICODE_QUOTE, "'")).replaceAll("\\\\'");
		return "'" + result + "'";
	};

	private static String renderDropFulltext(Index index, RenderConfig config, Neo4jVersion version) {
		if (RANGE_35_TO_42.contains(version)) {
			return String.format("CALL db.index.fulltext.drop(%s)", TO_LITERAL.apply(index.getName().getValue()));
		}
		else {
			return String.format("DROP %#s%s", new FormattableCatalogItem(index, config.getVersion()),
					config.ifNotExistsOrEmpty());
		}
	}

	private static String determineType(Index index, RenderConfig config) {

		if (index.getType() == Index.Type.PROPERTY && !config.useExplicitPropertyIndexType()) {
			return " ";
		}

		if (index.isBtreePropertyIndex()) {
			return " BTREE ";
		}
		else if (index.isRangePropertyIndex()) {
			return " RANGE ";
		}
		else {
			return " " + index.getType().name() + " ";
		}
	}

	private static String getAndEscapeIdentifier(Neo4jVersion version, Index index) {
		return getAndEscapeIdentifier(version, index, false);
	}

	private static String getAndEscapeIdentifier(Neo4jVersion version, Index index, boolean forceBackTicks) {

		if (RANGE_35_TO_42.contains(version) && !forceBackTicks) {
			return index.getDeconstructedIdentifiers().stream().map(TO_LITERAL).collect(Collectors.joining(", "));
		}
		else {
			return index.getDeconstructedIdentifiers()
				.stream()
				.map(version::sanitizeSchemaName)
				.collect(Collectors.joining("|"));
		}
	}

	@Override
	public void render(Index index, RenderConfig config, OutputStream target) throws IOException {

		Neo4jVersion version = config.getVersion();
		boolean isRelationshipPropertyIndex = index.getType() == Index.Type.PROPERTY
				&& index.getTargetEntityType() == TargetEntityType.RELATIONSHIP;

		if (!version.hasTextIndexes() && index.getType() == Index.Type.TEXT) {
			throw new IllegalArgumentException(
					String.format("The given index cannot be rendered on Neo4j %s", version));
		}

		if (isRelationshipPropertyIndex && RANGE_35_TO_42.contains(version)) {
			throw new IllegalArgumentException(
					String.format("The given relationship index cannot be rendered on Neo4j %s.", version));
		}
		if (config.isIdempotent() && (!version.hasIdempotentOperations()
				|| RANGE_35_TO_42.contains(version) && index.getType() == Index.Type.FULLTEXT)) {
			throw new IllegalArgumentException(
					String.format("The given index cannot be rendered in an idempotent fashion on Neo4j %s.", version));
		}

		if (!index.hasName() && config.isIdempotent() && config.getOperator() == Operator.DROP) {
			throw new IllegalArgumentException(
					"The index can only be rendered in the given context when having a name.");
		}

		Writer w = new BufferedWriter(new OutputStreamWriter(target, StandardCharsets.UTF_8));

		switch (index.getType()) {
			case POINT, PROPERTY, TEXT, VECTOR -> w.write(renderNodePropertiesIndex(index, config));
			case FULLTEXT -> w.write(renderFulltextIndex(index, config));
			default -> throw new IllegalArgumentException("Unsupported type of constraint: " + index.getType());
		}

		CypherRenderingUtils.renderOptions(index, config, w);
		w.flush();
	}

	private String renderFulltextIndex(Index index, RenderConfig config) {
		Operator operator = config.getOperator();
		Neo4jVersion version = config.getVersion();
		String indexName = index.getName().getValue();
		String identifier = getAndEscapeIdentifier(version, index);
		if (operator == Operator.CREATE) {
			return renderCreateFulltext(index, config, version, indexName, identifier);
		}
		else {
			return renderDropFulltext(index, config, version);
		}
	}

	private String renderCreateFulltext(Index index, RenderConfig config, Neo4jVersion version, String indexName,
			String identifier) {

		String safeName;
		if (RANGE_35_TO_42.contains(version)) {
			safeName = TO_LITERAL.apply(indexName);
		}
		else {
			safeName = config.getVersion().sanitizeSchemaName(indexName);
		}

		if (index.getTargetEntityType() == TargetEntityType.NODE) {
			String properties = renderFulltextProperties("n", index, config);
			if (RANGE_35_TO_42.contains(version)) {
				return String.format("CALL db.index.fulltext.createNodeIndex(%s,[%s],[%s])", safeName, identifier,
						properties);
			}
			else {
				return String.format("CREATE FULLTEXT INDEX %s %sFOR (n:%s) ON EACH [%s]", safeName,
						config.ifNotExistsOrEmpty(), identifier, properties);
			}
		}
		else {
			String properties = renderFulltextProperties("r", index, config);
			if (RANGE_35_TO_42.contains(version)) {
				return String.format("CALL db.index.fulltext.createRelationshipIndex(%s,[%s],[%s])", safeName,
						identifier, properties);
			}
			else {
				return String.format("CREATE FULLTEXT INDEX %s %sFOR ()-[r:%s]-() ON EACH [%s]", safeName,
						config.ifNotExistsOrEmpty(), identifier, properties);
			}
		}
	}

	private String renderNodePropertiesIndex(Index index, RenderConfig config) {

		Formattable item = formattablePropertyIndexItem(index, config);
		String identifier = getAndEscapeIdentifier(config.getVersion(), index, true);
		boolean isNodeIndex = index.getTargetEntityType() == TargetEntityType.NODE;

		String properties = isNodeIndex ? renderProperties("n", index, config) // node
				: renderProperties("r", index, config); // relationship

		Operator operator = config.getOperator();
		String type = determineType(index, config);

		Neo4jVersion version = config.getVersion();
		if (version == Neo4jVersion.V3_5) {
			return String.format("%s %#s ON :%s(%s)", operator, item, identifier, properties);
		}
		else if (operator == Operator.DROP) {
			if (!index.hasName() || config.isIgnoreName()) {
				if (index.getProperties().size() == 1) {
					return String.format("%s %#s ON :%s(%s)", operator, item, identifier, properties);
				}
				throw new IllegalStateException(
						String.format("Dropping an unnamed index is not supported on Neo4j %s.", version));
			}
			return String.format("%s %#s%s", operator, item, config.ifNotExistsOrEmpty());
		}
		else if (isNodeIndex) {
			return String.format("%s%s%#s %sFOR (n:%s) ON (%s)%s", operator, type, item, config.ifNotExistsOrEmpty(),
					identifier, properties,
					index.getOptionalOptions()
						.filter(ignored -> index.getType() == Index.Type.VECTOR)
						.map(o -> " OPTIONS " + o)
						.orElse(""));
		}
		else {
			return String.format("%s%s%#s %sFOR ()-[r:%s]-() ON (%s)", operator, type, item,
					config.ifNotExistsOrEmpty(), identifier, properties);
		}
	}

	private String renderProperties(String prefix, Index item, RenderConfig config) {

		Neo4jVersion version = config.getVersion();
		if (version == Neo4jVersion.V3_5 || config.getOperator() == Operator.DROP) {
			return item.getProperties().stream().map(version::sanitizeSchemaName).collect(Collectors.joining(", "));
		}
		else {
			return item.getProperties()
				.stream()
				.map(version::sanitizeSchemaName)
				.map(v -> prefix + "." + v)
				.collect(Collectors.joining(", "));
		}
	}

	private String renderFulltextProperties(String prefix, Index item, RenderConfig config) {
		if (RANGE_35_TO_42.contains(config.getVersion())) {
			return item.getProperties().stream().map(TO_LITERAL).collect(Collectors.joining(", "));
		}

		return item.getProperties().stream().map(v -> prefix + ".`" + v + "`").collect(Collectors.joining(", "));
	}

	Formattable formattablePropertyIndexItem(Index item, RenderConfig config) {
		return (config.isIgnoreName() || (config.getVersion() == Neo4jVersion.V3_5)) ? new AnonymousCatalogItem(item)
				: new FormattableCatalogItem(item, config.getVersion());
	}

}
