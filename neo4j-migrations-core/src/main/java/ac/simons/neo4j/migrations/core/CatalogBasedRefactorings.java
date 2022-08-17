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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.refactorings.Merge;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;
import ac.simons.neo4j.migrations.core.refactorings.Rename;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Set of factory methods to read various refactorings from XML files validating against {@code migrations.xsd}.
 *
 * @author Michael J. Simons
 * @soundtrack The Halo Effect - Days Of The Lost
 * @since 1.10.0
 */
final class CatalogBasedRefactorings {

	static Refactoring fromNode(Node node) {

		String type = Optional.ofNullable(node.getAttributes().getNamedItem("type")).map(Node::getNodeValue).orElse("");

		if (type.equals("merge.nodes")) {
			return createMerge(node, type);
		} else if (type.startsWith("rename.")) {
			return createRename(node, type);
		}

		throw createException(node, type, null);
	}

	private static Merge createMerge(Node node, String type) {
		String sourceQuery = findParameter(node, "sourceQuery").orElseThrow(
			() -> createException(node, type, "No source query"));
		List<Merge.PropertyMergePolicy> mergePolicies = findAllParameters(node, "mergePolicy")
			.stream().map(p -> {
				String pattern = null;
				Merge.PropertyMergePolicy.Strategy strategy = null;
				for (int i = 0; i < p.getChildNodes().getLength(); ++i) {
					Node item = p.getChildNodes().item(i);
					if ("pattern".equals(item.getNodeName())) {
						pattern = item.getTextContent().trim();
					} else if ("strategy".equals(item.getNodeName())) {
						strategy = Merge.PropertyMergePolicy.Strategy.valueOf(item.getTextContent().trim());
					}
				}
				if (pattern == null || strategy == null) {
					return null;
				}
				return Merge.PropertyMergePolicy.of(pattern, strategy);
			}).filter(Objects::nonNull).collect(Collectors.toList());
		return Merge.nodes(sourceQuery, mergePolicies);
	}

	private static Refactoring createRename(Node node, String type) {
		String op = type.split("\\.")[1];

		NodeList parameterList = findParameterList(node).orElseThrow(() ->
			createException(node, type, "The rename refactoring requires `from` and `to` parameters")
		);

		String from = findParameter(node, "from", parameterList).orElseThrow(() -> createException(node, type, "No `from` parameter"));
		String to = findParameter(node, "to", parameterList).orElseThrow(() -> createException(node, type, "No `to` parameter"));

		Rename rename;
		if ("type".equals(op)) {
			rename = Rename.type(from, to);
		} else if ("label".equals(op)) {
			rename =  Rename.label(from, to);
		} else if ("nodeProperty".equals(op)) {
			rename =  Rename.nodeProperty(from, to);
		} else if ("relationshipProperty".equals(op)) {
			rename =  Rename.relationshipProperty(from, to);
		} else {
			throw createException(node, type, String.format("`%s` is not a valid rename operation", op));
		}

		Optional<String> batchSize = findParameter(node, "batchSize", parameterList);
		if (batchSize.isPresent()) {
			try {
				rename = rename.inBatchesOf(Integer.parseInt(batchSize.get()));
			} catch (NumberFormatException nfe) {
				throw createException(node, type, "Invalid value `" + batchSize.get() + "` for parameter `batchSize", nfe);
			}
		}
		Optional<String> customQuery = findParameter(node, "customQuery", parameterList);
		if (customQuery.isPresent()) {
			rename = rename.withCustomQuery(customQuery.get());
		}
		return rename;
	}

	private static IllegalArgumentException createException(Node node, String type, String optionalMessage) {
		return createException(node, type, optionalMessage, null);
	}

	private static IllegalArgumentException createException(Node node, String type, String optionalMessage, Exception cause) {
		String typeAsAttribute =
			type == null || type.trim().isEmpty() ? "" : String.format(" type=\"%s\"", type.trim());
		String suffix = optionalMessage == null ? "" : ": " + optionalMessage;
		return new IllegalArgumentException(
			String.format("Cannot parse <%s%s /> into a supported refactoring%s", node.getNodeName(), typeAsAttribute,
				suffix), cause);
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

	private static Optional<String> findParameter(Node refactoring, String name, NodeList optionalParameters) {

		return (optionalParameters != null ? Optional.of(optionalParameters) : findParameterList(refactoring)).flatMap(parameters -> {
			for (int i = 0; i < parameters.getLength(); ++i) {
				Node parameter = parameters.item(i);
				if (!("parameter".equals(parameter.getNodeName()) && parameter.hasAttributes())) {
					continue;
				}
				Node parameterName = parameter.getAttributes().getNamedItem("name");
				if (parameterName != null && name.equals(parameterName.getNodeValue())) {
					return Optional.ofNullable(parameter.getTextContent()).map(String::trim).filter(v -> !v.isEmpty());
				}
			}
			return Optional.empty();
		});
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

	private CatalogBasedRefactorings() {
	}
}
