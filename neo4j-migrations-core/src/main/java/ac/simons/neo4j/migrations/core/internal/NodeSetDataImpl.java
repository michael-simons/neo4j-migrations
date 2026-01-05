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
package ac.simons.neo4j.migrations.core.internal;

import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.NodeSetData;

import org.w3c.dom.Node;

/**
 * A simple implementation of {@link NodeSetData}, working on a flat list of {@link Node
 * nodes}. {@link NodeSetData} does not have any type parameters on JDK 8, it does have
 * type parameters on JDK 17. This project needs JDK 17 to compile but still targets JDK
 * 8. At least IntelliJ gets this wrong (even with {@literal --release 8} for the main
 * source) and issues wrong warnings.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
@SuppressWarnings("rawtypes") // See JavaDoc comment above…
public final class NodeSetDataImpl implements NodeSetData {

	private final List<Node> elements;

	private NodeSetDataImpl(List<Node> elements) {
		this.elements = elements;
	}

	/**
	 * Build a {@link NodeSetData} from the given list of elements.
	 * @param elements the actual nodes
	 * @return an instance of {@link NodeSetData}
	 */
	public static NodeSetData of(List<Node> elements) {
		return new NodeSetDataImpl(elements);
	}

	@Override
	public Iterator<Node> iterator() {
		return this.elements.iterator();
	}

}
