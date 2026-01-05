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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ac.simons.neo4j.migrations.core.refactorings.DefaultListToVector.Target;

/**
 * This refactoring allows converting vector properties that have been created with either
 * <code>db.create.setNodeVectorProperty</code> or
 * <code>db.create.setRelationshipVectorPropert</code> to the new actual vector property
 * type available in Neo4j 2025.10.
 * <p>
 * Using any of the above procedures will create entity properties of type
 * <code>LIST&lt;FLOAT NOT NULL&gt; NOT NULL</code> which are not compatible with the
 * <code>VECTOR</code> type. This migration will convert the former into the latter,
 * opening up the data-model for all further improvements for vector indexes and vector
 * searches in Neo4j to come.
 * <p>
 * Only properties of type <code>LIST&lt;FLOAT NOT NULL&gt; NOT NULL</code> will be
 * matched and converted by default. If this is not what you want, configure a custom
 * query via {@link #onNodesMatching(String)} or {@link #onRelationshipsMatching(String)}.
 * <p>
 * All queries will be prefixed with <code>CYPHER 25</code>.
 *
 * @author Michael J. Simons
 * @since 3.0.0
 */
public sealed interface ListToVector extends CustomizableRefactoring<ListToVector> permits DefaultListToVector {

	/**
	 * The default property name holding the embedding.
	 */
	String DEFAULT_PROPERTY_NAME = "embedding";

	static ListToVector onNodes(String primaryLabel, String... additionalLabels) {
		List<String> labels = new ArrayList<>();
		labels.add(Objects.requireNonNull(primaryLabel, "Primary label is required"));
		if (additionalLabels != null) {
			Arrays.stream(additionalLabels).filter(Objects::nonNull).forEach(labels::add);
		}
		return new DefaultListToVector(Target.NODE, labels, null, null, null, null);
	}

	static ListToVector onRelationships(String type) {
		return new DefaultListToVector(Target.RELATIONSHIP, List.of(Objects.requireNonNull(type, "Type is required")),
				null, null, null, null);
	}

	static ListToVector onNodesMatching(String query) {
		return new DefaultListToVector(Target.NODE, null, null, query, null, null);
	}

	static ListToVector onRelationshipsMatching(String query) {
		return new DefaultListToVector(Target.RELATIONSHIP, null, null, query, null, null);
	}

	/**
	 * Alters the name of the property that should be converted. The default is
	 * {@code #DEFAULT_PROPERTY_NAME}.
	 * @param name the new name of the graph property that should be turned into a
	 * <code>VECTOR</code>
	 * @return the refactoring ready to use
	 */
	ListToVector withProperty(String name);

	/**
	 * Configures the inner vector type tp be used, maybe any of the supported inner
	 * types: <a href=
	 * "https://neo4j.com/docs/cypher-manual/current/values-and-types/vector/#valid-values">Valid
	 * values for vectors</a>.
	 * @param type the vector type to use
	 * @return the refactoring ready to use
	 */
	ListToVector withElementType(ElementType type);

	/**
	 * Support inner vector types.
	 */
	enum ElementType {

		/**
		 * Neo4j INTEGER8 type.
		 */
		INTEGER8,
		/**
		 * Neo4j INTEGER16 type.
		 */
		INTEGER16,
		/**
		 * Neo4j INTEGER32 type.
		 */
		INTEGER32,
		/**
		 * Neo4j INTEGER64 type (Cypher CIP-200 normalises INTEGER64 to INTEGER, and we
		 * want the JDBC driver to be aligned here).
		 */
		INTEGER,
		/**
		 * Neo4j FLOAT32 type.
		 */
		FLOAT32,
		/**
		 * Neo4j FLOAT64 type.
		 */
		FLOAT

	}

}
