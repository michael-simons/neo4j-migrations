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
package ac.simons.neo4j.migrations.core.refactorings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Relationship;

/**
 * A refactoring that merges two or more nodes including their labels, properties and
 * relationships.
 *
 * @author Michael J. Simons
 * @since 1.10.0
 */
final class DefaultMerge implements Merge {

	/**
	 * The query representing the source nodes to be merged. This query must return one or
	 * more rows with one single element of type node per row.
	 */
	private final String query;

	private final List<PropertyMergePolicy> mergePolicies;

	DefaultMerge(String query, List<PropertyMergePolicy> mergePolicies) {
		this.query = query;
		this.mergePolicies = mergePolicies;
	}

	private static Optional<Query> generateLabelCopyQuery(UnaryOperator<String> sanitizer, QueryRunner queryRunner,
			Ids ids) {

		String queryForLabels = """
				MATCH (n) WHERE elementId(n) IN $ids
				UNWIND labels(n) AS label
				WITH DISTINCT label
				ORDER BY label ASC
				RETURN collect(label) AS labels""";

		List<String> labels = queryRunner.run(new Query(queryForLabels, Collections.singletonMap("ids", ids.tail)))
			.single()
			.get("labels")
			.asList(Value::asString);
		if (labels.isEmpty()) {
			return Optional.empty();
		}
		String literals = labels.stream().map(sanitizer).collect(Collectors.joining(":", ":", ""));

		return Optional.of(new Query(String.format("MATCH (n) WHERE elementId(n) = $id SET n%s", literals),
				Collections.singletonMap("id", ids.first)));
	}

	private static Optional<Query> generatePropertyCopyQuery(QueryRunner queryRunner, Ids ids,
			List<PropertyMergePolicy> policies) {

		String queryForProperties = """
				UNWIND $ids AS id
				MATCH (n) WHERE elementId(n) = id
				UNWIND keys(n) AS key
				WITH key, n[key] as value
				 WITH key, collect(value) AS values
				RETURN {key: key, values: values} AS property
				ORDER BY property.key ASC""";

		List<Map<String, ?>> rows = queryRunner
			.run(new Query(queryForProperties, Collections.singletonMap("ids", ids.value)))
			.list(MapAccessor::asMap);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		// using insertion order for maps so that query generation is deterministic
		Map<String, Object> combinedProperties = new LinkedHashMap<>(rows.size());
		for (Map<String, ?> row : rows) {
			for (Map.Entry<String, ?> entry : row.entrySet()) {
				@SuppressWarnings("unchecked")
				Map<String, Object> property = (Map<String, Object>) entry.getValue();
				String propertyName = (String) property.get("key");
				@SuppressWarnings("unchecked")
				List<Object> aggregatedPropertyValues = (List<Object>) property.get("values");
				Object value = findPolicy(policies, propertyName)
					.orElseThrow(() -> new IllegalStateException(
							String.format("Could not find merge policy for node property `%s`", propertyName)))
					.apply(aggregatedPropertyValues);
				combinedProperties.put(propertyName, value);
			}
		}

		return Optional.of(new Query("MATCH (n) WHERE elementId(n) = $id SET n = $properties",
				Values.parameters("id", ids.first, "properties", combinedProperties)));
	}

	private static Optional<Query> generateRelationshipCopyQuery(UnaryOperator<String> sanitizer,
			QueryRunner queryRunner, Ids ids) {

		String queryForRels = """
				MATCH (n) WHERE elementId(n) IN $ids
				WITH [ (n)-[r]-() | r ] AS rels
				UNWIND rels AS rel
				RETURN DISTINCT rel, elementId(startNode(rel)) as startId, elementId(endNode(rel)) as endId
				ORDER BY type(rel) ASC, elementId(rel) ASC""";

		List<Record> rows = queryRunner.run(new Query(queryForRels, Collections.singletonMap("ids", ids.tail))).list();
		if (rows.isEmpty()) {
			return Optional.empty();
		}

		StringBuilder query = new StringBuilder("MATCH (target) WHERE elementId(target) = $0 ");

		Map<String, Object> parameters = new HashMap<>();
		int parameterIndex = 0;
		parameters.put(String.valueOf(parameterIndex++), ids.first);
		parameterIndex++;
		for (Record row : rows) {
			Relationship relation = row.get("rel").asRelationship();
			parameters.put(String.valueOf(parameterIndex), relation.asMap());

			String startId = row.get("startId").asString();
			String endId = row.get("endId").asString();
			String relationshipType = sanitizer.apply(relation.type());
			if (ids.tail.contains(startId) && ids.tail.contains(endId)) { // current or
																			// post-merge
																			// self-rel
				query
					.append(String.format("WITH target CREATE (target)-[rel_%1$d:%2$s]->(target) SET rel_%1$d = $%3$d ",
							parameterIndex, relationshipType, parameterIndex));
				parameterIndex++;
				continue;
			}
			if (ids.tail.contains(endId)) { // incoming
				parameters.put(String.valueOf(parameterIndex + 1), startId);
				query.append(String.format("WITH target MATCH (n_%1$d) WHERE elementId(n_%1$d) = $%1$d ",
						parameterIndex + 1));
				query.append(String.format("CREATE (n_%1$d)-[rel_%1$d:%2$s]->(target) SET rel_%1$d = $%3$d ",
						parameterIndex + 1, relationshipType, parameterIndex));
			}
			else { // outgoing
				parameters.put(String.valueOf(parameterIndex + 1), endId);
				query.append(String.format("WITH target MATCH (n_%1$d) WHERE elementId(n_%1$d) = $%1$d ",
						parameterIndex + 1));
				query.append(String.format("CREATE (n_%1$d)<-[rel_%1$d:%2$s]-(target) SET rel_%1$d = $%3$d ",
						parameterIndex + 1, relationshipType, parameterIndex));
			}
			parameterIndex += 2;
		}

		return Optional.of(new Query(query.toString(), parameters));
	}

	private static Optional<Query> generateNodeDeletion(Ids ids) {
		return Optional.of(new Query(("MATCH (n) WHERE elementId(n) IN $ids DETACH DELETE n"),
				Collections.singletonMap("ids", ids.tail)));
	}

	private static Optional<PropertyMergePolicy> findPolicy(List<PropertyMergePolicy> policies, String propertyName) {
		return policies.stream().filter(policy -> policy.pattern().matcher(propertyName).matches()).findFirst();
	}

	@Override
	public Counters apply(RefactoringContext context) {

		try (QueryRunner tx = context.getQueryRunner(QueryRunner.defaultFeatureSet().withElementIdSupport(true))) {

			String element = context.findSingleResultIdentifier(this.query).orElseThrow(IllegalArgumentException::new);

			Ids ids = Ids.of(tx
				.run(new Query(String.format("CALL { %s } WITH %s RETURN collect(elementId(%2$s)) AS ids", this.query,
						element)))
				.single()
				.get(0)
				.asList(Value::asString));
			if (ids.size() < 2) {
				return Counters.empty();
			}

			Function<Query, Counters> runAndConsume = q -> new DefaultCounters(tx.run(q).consume().counters());

			List<Counters> results = new ArrayList<>(4);
			generateLabelCopyQuery(context::sanitizeSchemaName, tx, ids).map(runAndConsume).ifPresent(results::add);
			generatePropertyCopyQuery(tx, ids, this.mergePolicies).map(runAndConsume).ifPresent(results::add);
			generateRelationshipCopyQuery(context::sanitizeSchemaName, tx, ids).map(runAndConsume)
				.ifPresent(results::add);
			generateNodeDeletion(ids).map(runAndConsume).ifPresent(results::add);

			return results.stream().reduce(Counters.empty(), Counters::add);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultMerge that = (DefaultMerge) o;
		return this.query.equals(that.query) && this.mergePolicies.equals(that.mergePolicies);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.query, this.mergePolicies);
	}

	private record Ids(List<String> value, String first, List<String> tail) {

		static Ids of(List<String> ids) {
			if (ids.isEmpty()) {
				return new Ids(Collections.emptyList(), null, Collections.emptyList());
			}
			else {
				return new Ids(ids, ids.get(0),
						(ids.size() == 1) ? Collections.emptyList() : ids.subList(1, ids.size()));
			}
		}

		int size() {
			return this.value.size();
		}
	}

}
