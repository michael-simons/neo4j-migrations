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

import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Formattable;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Renders indexes (supported operators are {@link Operator#CREATE} and {@link Operator#DROP}) as Cypher.
 *
 * @author Gerrit Meier
 */
enum IndexToCypherRenderer implements Renderer<Index> {

	INSTANCE;

	private static final BiFunction<String, String, Collector<CharSequence, ?, String>> joiningCollectorFunction =
			(separator, escapeCharacter) ->
					Collectors.joining(escapeCharacter + separator + escapeCharacter, escapeCharacter, escapeCharacter);

	private static final Collector<CharSequence, ?, String> pipeBacktickJoiningCollector = joiningCollectorFunction.apply("|", "`");
	private static final Collector<CharSequence, ?, String> commaBacktickJoiningCollector = joiningCollectorFunction.apply(", ", "`");
	private static final Collector<CharSequence, ?, String> commaQuoteJoiningCollector = joiningCollectorFunction.apply(", ", "'");

	@Override
	public void render(Index index, RenderConfig context, OutputStream target) throws IOException {

		Neo4jVersion version = context.getVersion();
		boolean isRelationshipPropertyIndex = index.getType() == Index.Type.PROPERTY && index.getTargetEntityType() == TargetEntityType.RELATIONSHIP;
		if (isRelationshipPropertyIndex && Neo4jVersion.RANGE_35_TO_42.contains(version)) {
			throw new IllegalStateException(
				String.format("The given relationship index cannot be rendered on Neo4j %s.",
					version));
		}
		if (context.isIdempotent() &&
				(!version.hasIdempotentOperations() || Neo4jVersion.RANGE_35_TO_42.contains(version) && index.getType() == Index.Type.FULLTEXT)) {
			throw new IllegalStateException(
				String.format("The given index cannot be rendered in an idempotent fashion on Neo4j %s.",
					version));
		}

		if (!index.hasName() && context.isIdempotent() && context.getOperator() == Operator.DROP) {
			throw new IllegalStateException("The constraint can only be rendered in the given context when having a name.");
		}

		Writer w = new BufferedWriter(new OutputStreamWriter(target, StandardCharsets.UTF_8));
		if (context.getOperator() == Operator.DROP && context.getVersion() != Neo4jVersion.V3_5 && index.hasName() && !context.isIgnoreName()) {
			w.write(String.format("DROP %#s%s", index, ifNotExistsOrEmpty(context)));
		} else {
			switch (index.getType()) {
				case PROPERTY:
					w.write(renderNodePropertiesIndex(index, context));
					break;
				case FULLTEXT:
					w.write(renderFulltextIndex(index, context));
					break;
				default:
					throw new IllegalArgumentException("Unsupported type of constraint: " + index.getType());
			}
		}
		w.flush();
	}

	private String renderFulltextIndex(Index index, RenderConfig config) {
		Operator operator = config.getOperator();
		Neo4jVersion version = config.getVersion();
		String indexName = index.getName().getValue();
		if (operator == Operator.CREATE) {
			if (index.getTargetEntityType() == TargetEntityType.NODE) {
				String properties = renderFulltextProperties("n", index, config);
				if (Neo4jVersion.RANGE_35_TO_42.contains(version)) {
					String identifier = Arrays.stream(index.getIdentifier().split("\\|")).collect(commaQuoteJoiningCollector);
					return String.format("CALL db.index.fulltext.createNodeIndex('%s',[%s],[%s])", indexName, identifier, properties);
				} else {
					String identifier = Arrays.stream(index.getIdentifier().split("\\|")).collect(pipeBacktickJoiningCollector);
					return String.format("CREATE FULLTEXT INDEX %s %sFOR (n:%s) ON EACH [%s]", indexName, ifNotExistsOrEmpty(config), identifier, properties);
				}
			} else {
				String properties = renderFulltextProperties("r", index, config);
				if (Neo4jVersion.RANGE_35_TO_42.contains(version)) {
					String identifier = Arrays.stream(index.getIdentifier().split("\\|")).collect(commaQuoteJoiningCollector);
					return String.format("CALL db.index.fulltext.createRelationshipIndex('%s',[%s],[%s])", indexName, identifier, properties);
				} else {
					String identifier = Arrays.stream(index.getIdentifier().split("\\|")).collect(pipeBacktickJoiningCollector);
					return String.format("CREATE FULLTEXT INDEX %s %sFOR ()-[r:%s]-() ON EACH [%s]", indexName, ifNotExistsOrEmpty(config), identifier, properties);
				}
			}

		}
		return "not implemented";
	}

	private static String ifNotExistsOrEmpty(RenderConfig context) {
		switch (context.getOperator()) {
			case CREATE:
				return context.isIdempotent() ? "IF NOT EXISTS " : "";
			case DROP:
				return context.isIdempotent() ? " IF EXISTS" : "";
		}
		throw new IllegalStateException();
	}

	private String renderNodePropertiesIndex(Index index, RenderConfig config) {

		Formattable item = formattablePropertyIndexItem(index, config);
		String identifier = "`" + index.getIdentifier() + "`";
		boolean isNodeIndex = index.getTargetEntityType() == TargetEntityType.NODE;

		String properties = isNodeIndex
				? renderProperties("n", index, config)  // node
				: renderProperties("r", index, config); // relationship

		Operator operator = config.getOperator();

		Neo4jVersion version = config.getVersion();
		if (operator == Operator.DROP) {
			if (index.getProperties().size() == 1 || version == Neo4jVersion.V3_5) {
				return String.format("%s %#s ON :%s(%s)", operator, item, identifier, properties);
			}
			throw new IllegalStateException(
					String.format("Dropping an unnamed index is not supported on Neo4j %s.", version));
		} else if (operator == Operator.CREATE) {
			if (version == Neo4jVersion.V3_5) {
				return String.format("%s %#s ON :%s(%s)", operator, item, identifier, properties);
			} else {
				if (isNodeIndex) {
					return String.format("%s %#s %sFOR (n:%s) ON (%s)", operator, item, ifNotExistsOrEmpty(config),
							identifier, properties);
				} else {
					return String.format("%s %#s %sFOR ()-[r:%s]-() ON (%s)", operator, item, ifNotExistsOrEmpty(config),
							identifier, properties);
				}
			}
		}

		throw new UnsupportedOperationException("Operator " + operator + " is not supported.");
	}

	private String renderProperties(String prefix, Index item, RenderConfig config) {

		if (config.getVersion() == Neo4jVersion.V3_5 || config.getOperator() == Operator.DROP) {
			return item.getProperties().stream().collect(commaBacktickJoiningCollector);
		} else {
			return item.getProperties().stream().map(v -> prefix + ".`" + v + "`").collect(Collectors.joining(", "));
		}
	}

	private String renderFulltextProperties(String prefix, Index item, RenderConfig config) {
		if (Neo4jVersion.RANGE_35_TO_42.contains(config.getVersion())) {
			return item.getProperties().stream().collect(commaQuoteJoiningCollector);
		}

		return item.getProperties().stream().map(v -> prefix + ".`" + v + "`").collect(Collectors.joining(", "));
	}

	Formattable formattablePropertyIndexItem(Index item, RenderConfig config) {
		return config.isIgnoreName() || config.getVersion() == Neo4jVersion.V3_5 ? new AnonymousCatalogItem(item) : item;
	}
}
