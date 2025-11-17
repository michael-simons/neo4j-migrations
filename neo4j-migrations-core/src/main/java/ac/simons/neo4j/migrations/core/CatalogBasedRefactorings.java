/*
 * Copyright 2020-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.refactorings.AddSurrogateKey;
import ac.simons.neo4j.migrations.core.refactorings.CustomizableRefactoring;
import ac.simons.neo4j.migrations.core.refactorings.ListToVector;
import ac.simons.neo4j.migrations.core.refactorings.Merge;
import ac.simons.neo4j.migrations.core.refactorings.MigrateBTreeIndexes;
import ac.simons.neo4j.migrations.core.refactorings.Normalize;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;
import ac.simons.neo4j.migrations.core.refactorings.Rename;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Set of factory methods to read various refactorings from XML files validating against
 * {@code migrations.xsd}.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
final class CatalogBasedRefactorings {

	private static final String PARAMETER_NAME_CUSTOM_QUERY = "customQuery";

	private static final String PARAMETER_NAME_PROPERTY = "property";

	private CatalogBasedRefactorings() {
	}

	static Refactoring fromNode(Node node) {

		String type = Optional.ofNullable(node.getAttributes().getNamedItem("type")).map(Node::getNodeValue).orElse("");

		if (type.equals("merge.nodes")) {
			return createMerge(node, type);
		}
		else if (type.equals("migrate.createFutureIndexes")) {
			return createMigrateBtreeIndexes(node, false);
		}
		else if (type.equals("migrate.replaceBTreeIndexes")) {
			return createMigrateBtreeIndexes(node, true);
		}
		else if (type.startsWith("rename.")) {
			return createRename(node, type);
		}
		else if (type.equals("normalize.asBoolean")) {
			return createNormalizeAsBoolean(node, type);
		}
		else if (type.startsWith("addSurrogateKeyTo.")) {
			return addSurrogateKey(node, type);
		}
		else if (type.startsWith("listToVectorOn.")) {
			return listToVector(node, type);
		}

		throw createException(node, type, null);
	}

	private static AddSurrogateKey addSurrogateKey(Node node, String type) {
		String op = type.split("\\.")[1];

		NodeList parameterList = findParameterList(node).orElseThrow(
				() -> createException(node, type, "The addSurrogateKey refactoring requires several parameters"));

		Optional<String> optionalCustomQuery = findParameter(node, PARAMETER_NAME_CUSTOM_QUERY, parameterList);
		AtomicReference<AddSurrogateKey> refactoring = new AtomicReference<>();
		if ("nodes".equals(op)) {
			findParameterValues(parameterList, "labels").filter(Predicate.not(List::isEmpty))
				.ifPresentOrElse(labels -> refactoring.set(AddSurrogateKey.toNodes(labels.get(0),
						labels.subList(1, labels.size()).toArray(String[]::new))), () -> {
							if (optionalCustomQuery.isPresent()) {
								refactoring.set(AddSurrogateKey.toNodesMatching(optionalCustomQuery.get().trim()));
							}
							else {
								throw createException(node, type, "No labels specified");
							}
						});
		}
		else if ("relationships".equals(op)) {
			findParameter(node, "type", parameterList).ifPresentOrElse(
					relationshipType -> refactoring.set(AddSurrogateKey.toRelationships(relationshipType)), () -> {
						if (optionalCustomQuery.isPresent()) {
							refactoring.set(AddSurrogateKey.toRelationshipsMatching(optionalCustomQuery.get().trim()));
						}
						else {
							throw createException(node, type, "No `type` parameter");
						}
					});
		}
		else {
			throw createException(node, type, String.format("`%s` is not a valid rename operation", op));
		}

		refactoring.updateAndGet(
				r -> findParameter(node, PARAMETER_NAME_PROPERTY, parameterList).map(p -> r.withProperty(p.trim()))
					.orElse(r));
		refactoring.updateAndGet(
				r -> findParameter(node, "generatorFunction", parameterList).map(p -> r.withGeneratorFunction(p.trim()))
					.orElse(r));

		return customize(refactoring.get(), node, type, parameterList);
	}

	private static ListToVector listToVector(Node node, String type) {
		String op = type.split("\\.")[1];

		NodeList parameterList = findParameterList(node)
			.orElseThrow(() -> createException(node, type, "The listToVector refactoring requires several parameters"));

		Optional<String> optionalCustomQuery = findParameter(node, PARAMETER_NAME_CUSTOM_QUERY, parameterList);
		AtomicReference<ListToVector> refactoring = new AtomicReference<>();
		if ("nodes".equals(op)) {
			findParameterValues(parameterList, "labels").filter(Predicate.not(List::isEmpty))
				.ifPresentOrElse(labels -> refactoring
					.set(ListToVector.onNodes(labels.get(0), labels.subList(1, labels.size()).toArray(String[]::new))),
						() -> {
							if (optionalCustomQuery.isPresent()) {
								refactoring.set(ListToVector.onNodesMatching(optionalCustomQuery.get().trim()));
							}
							else {
								throw createException(node, type, "No labels specified");
							}
						});
		}
		else if ("relationships".equals(op)) {
			findParameter(node, "type", parameterList).ifPresentOrElse(
					relationshipType -> refactoring.set(ListToVector.onRelationships(relationshipType)), () -> {
						if (optionalCustomQuery.isPresent()) {
							refactoring.set(ListToVector.onRelationshipsMatching(optionalCustomQuery.get().trim()));
						}
						else {
							throw createException(node, type, "No `type` parameter");
						}
					});
		}
		else {
			throw createException(node, type, String.format("`%s` is not a valid listToVector operation", op));
		}

		refactoring.updateAndGet(
				r -> findParameter(node, PARAMETER_NAME_PROPERTY, parameterList).map(p -> r.withProperty(p.trim()))
					.orElse(r));
		refactoring.updateAndGet(r -> findParameter(node, "elementType", parameterList)
			.map(p -> r.withElementType(ListToVector.ElementType.valueOf(p.trim().toUpperCase(Locale.ROOT))))
			.orElse(r));

		return customize(refactoring.get(), node, type, parameterList);
	}

	private static Refactoring createMigrateBtreeIndexes(Node node, boolean drop) {

		Optional<NodeList> optionalParameters = findParameterList(node);
		MigrateBTreeIndexes migrateBTreeIndexes;
		if (drop) {
			migrateBTreeIndexes = MigrateBTreeIndexes.replaceBTreeIndexes();
		}
		else {
			String suffix = optionalParameters.flatMap(parameters -> findParameter(node, "suffix", parameters))
				.orElse(null);
			migrateBTreeIndexes = MigrateBTreeIndexes.createFutureIndexes(suffix);
		}

		List<String> excludes = optionalParameters.flatMap(parameters -> findParameterValues(parameters, "excludes"))
			.orElse(Collections.emptyList());
		List<String> includes = optionalParameters.flatMap(parameters -> findParameterValues(parameters, "includes"))
			.orElse(Collections.emptyList());
		Map<String, Index.Type> typeMappings = optionalParameters
			.flatMap(parameters -> findParameterNode(parameters, "typeMapping"))
			.map(typeMapping -> findChildNodes(typeMapping, "mapping"))
			.map(mappings -> mappings.stream()
				.collect(Collectors.toMap(
						mapping -> findChildNode(mapping, "name").map(Node::getTextContent)
							.map(String::trim)
							.orElseThrow(() -> new IllegalArgumentException(
									"Index name is required when customizing type mappings")),
						mapping -> findChildNode(mapping, "type").map(Node::getTextContent)
							.map(String::trim)
							.map(Index.Type::valueOf)
							.orElseThrow(() -> new IllegalArgumentException(
									"Type is required when customizing type mappings")))))
			.orElse(Collections.emptyMap());

		return migrateBTreeIndexes.withExcludes(excludes).withIncludes(includes).withTypeMapping(typeMappings);
	}

	private static Normalize createNormalizeAsBoolean(Node node, String type) {

		NodeList parameterList = findParameterList(node).orElseThrow(() -> createException(node, type,
				"The normalizeAsBoolean refactoring requires `property`, `trueValues` and `falseValues` parameters"));

		String property = findParameter(node, PARAMETER_NAME_PROPERTY, parameterList)
			.orElseThrow(() -> createException(node, type, "No `property` parameter"));

		Collection<String> rawTrueValues = findParameterValues(parameterList, "trueValues")
			.orElseThrow(() -> createException(node, type, "No `trueValues` parameter"));

		Collection<String> rawFalseValues = findParameterValues(parameterList, "falseValues")
			.orElseThrow(() -> createException(node, type, "No `falseValues` parameter"));

		Function<String, Object> mapToType = value -> {
			try {
				if (value == null || "null".equals(value)) {
					return null;
				}
				return Long.parseLong(value);
			}
			catch (NumberFormatException ex) {
				return value;
			}
		};
		List<Object> trueValues = rawTrueValues.stream().map(mapToType).toList();
		List<Object> falseValues = rawFalseValues.stream().map(mapToType).toList();

		Normalize normalize = Normalize.asBoolean(property, trueValues, falseValues);
		return customize(normalize, node, type, parameterList);
	}

	private static <T extends CustomizableRefactoring<T>> T customize(T refactoring, Node node, String type,
			NodeList parameterList) {
		Optional<String> batchSize = findParameter(node, "batchSize", parameterList);
		T result = refactoring;
		if (batchSize.isPresent()) {
			try {
				result = result.inBatchesOf(Integer.parseInt(batchSize.get()));
			}
			catch (NumberFormatException nfe) {
				throw createException(node, type, "Invalid value `" + batchSize.get() + "` for parameter `batchSize`",
						nfe);
			}
		}
		Optional<String> customQuery = findParameter(node, PARAMETER_NAME_CUSTOM_QUERY, parameterList);
		if (customQuery.isPresent()) {
			result = result.withCustomQuery(customQuery.get());
		}
		return result;
	}

	private static Merge createMerge(Node node, String type) {
		String sourceQuery = findParameter(node, "sourceQuery")
			.orElseThrow(() -> createException(node, type, "No source query"));
		List<Merge.PropertyMergePolicy> mergePolicies = findAllParameters(node, "mergePolicy").stream().map(p -> {
			String pattern = null;
			Merge.PropertyMergePolicy.Strategy strategy = null;
			for (int i = 0; i < p.getChildNodes().getLength(); ++i) {
				Node item = p.getChildNodes().item(i);
				if ("pattern".equals(item.getNodeName())) {
					pattern = item.getTextContent().trim();
				}
				else if ("strategy".equals(item.getNodeName())) {
					strategy = Merge.PropertyMergePolicy.Strategy.valueOf(item.getTextContent().trim());
				}
			}
			if (pattern == null || strategy == null) {
				return null;
			}
			return Merge.PropertyMergePolicy.of(pattern, strategy);
		}).filter(Objects::nonNull).toList();
		return Merge.nodes(sourceQuery, mergePolicies);
	}

	private static Refactoring createRename(Node node, String type) {
		String op = type.split("\\.")[1];

		NodeList parameterList = findParameterList(node).orElseThrow(
				() -> createException(node, type, "The rename refactoring requires `from` and `to` parameters"));

		String from = findParameter(node, "from", parameterList)
			.orElseThrow(() -> createException(node, type, "No `from` parameter"));
		String to = findParameter(node, "to", parameterList)
			.orElseThrow(() -> createException(node, type, "No `to` parameter"));

		Rename rename;
		if ("type".equals(op)) {
			rename = Rename.type(from, to);
		}
		else if ("label".equals(op)) {
			rename = Rename.label(from, to);
		}
		else if ("nodeProperty".equals(op)) {
			rename = Rename.nodeProperty(from, to);
		}
		else if ("relationshipProperty".equals(op)) {
			rename = Rename.relationshipProperty(from, to);
		}
		else {
			throw createException(node, type, String.format("`%s` is not a valid rename operation", op));
		}

		return customize(rename, node, type, parameterList);
	}

	private static IllegalArgumentException createException(Node node, String type, String optionalMessage) {
		return createException(node, type, optionalMessage, null);
	}

	private static IllegalArgumentException createException(Node node, String type, String optionalMessage,
			Exception cause) {
		String typeAsAttribute = (type == null || type.trim().isEmpty()) ? ""
				: String.format(" type=\"%s\"", type.trim());
		String suffix = (optionalMessage != null) ? (": " + optionalMessage) : "";
		return new IllegalArgumentException(String.format("Cannot parse <%s%s /> into a supported refactoring%s",
				node.getNodeName(), typeAsAttribute, suffix), cause);
	}

	private static Optional<NodeList> findParameterList(Node refactoring) {
		NodeList parameters = null;
		NodeList refactoringChildNodes = refactoring.getChildNodes();
		for (int i = 0; i < refactoringChildNodes.getLength(); ++i) {
			if ("parameters".equals(refactoringChildNodes.item(i).getNodeName())) {
				parameters = refactoringChildNodes.item(i).getChildNodes();
				break;
			}
		}
		return Optional.ofNullable(parameters);
	}

	private static Optional<String> findParameter(Node refactoring, String name) {
		return findParameter(refactoring, name, null);
	}

	private static Optional<Node> findParameterNode(NodeList parameters, String name) {
		for (int i = 0; i < parameters.getLength(); ++i) {
			Node parameter = parameters.item(i);
			if (!("parameter".equals(parameter.getNodeName()) && parameter.hasAttributes())) {
				continue;
			}
			Node parameterName = parameter.getAttributes().getNamedItem("name");
			if (parameterName != null && name.equals(parameterName.getNodeValue())) {
				return Optional.of(parameter);
			}
		}
		return Optional.empty();
	}

	private static Optional<String> findParameter(Node refactoring, String name, NodeList optionalParameters) {

		return Optional.ofNullable(optionalParameters)
			.or(() -> findParameterList(refactoring))
			.flatMap(parameters -> findParameterNode(parameters, name))
			.map(Node::getTextContent)
			.map(String::trim)
			.filter(v -> !v.isEmpty());
	}

	private static List<Node> findAllParameters(Node refactoring, String name) {

		return findParameterList(refactoring).map(parameters -> {
			List<Node> result = new ArrayList<>();
			for (int i = 0; i < parameters.getLength(); ++i) {
				Node parameter = parameters.item(i);
				if (!("parameter".equals(parameter.getNodeName()) && parameter.hasAttributes())) {
					continue;
				}
				Node parameterName = parameter.getAttributes().getNamedItem("name");
				if (parameterName != null && name.equals(parameterName.getNodeValue())) {
					result.add(parameter);
				}
			}
			return result;
		}).orElseGet(Collections::emptyList);
	}

	private static Optional<List<String>> findParameterValues(NodeList parametersNodeList, String parameterNameToFind) {

		if (parametersNodeList == null) {
			return Optional.empty();
		}

		Function<Node, List<String>> aggregateValues = parameterNode -> {
			List<String> values = new ArrayList<>();
			NodeList childNodes = parameterNode.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); ++i) {
				Node childNode = childNodes.item(i);
				if ("value".equals(childNode.getNodeName())) {
					if (!childNode.hasChildNodes()) {
						values.add(null);
					}
					else {
						values.add(childNode.getTextContent());
					}
				}
			}
			return values;
		};

		return findParameterNode(parametersNodeList, parameterNameToFind).map(aggregateValues);
	}

	private static Optional<Node> findChildNode(Node node, String childNodeName) {
		NodeList childNodes = node.getChildNodes();
		Optional<Node> result = Optional.empty();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node childNode = childNodes.item(i);
			if (!childNodeName.equals(childNode.getNodeName())) {
				continue;
			}
			if (result.isPresent()) {
				throw new IllegalArgumentException("Duplicate child node `" + childNodeName + "`");
			}
			result = Optional.of(childNode);
		}
		return result;
	}

	private static List<Node> findChildNodes(Node node, String childNodeName) {
		NodeList childNodes = node.getChildNodes();
		List<Node> result = new ArrayList<>();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node childNode = childNodes.item(i);
			if (childNodeName.equals(childNode.getNodeName())) {
				result.add(childNode);
			}
		}
		return result;
	}

}
