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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An operation on items of a catalog.
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
final class Operation {

	/**
	 * The operator to this operation.
	 */
	private final Operator operator;

	/**
	 * Zero or more items to operate on. If the list is empty, the operator will be applied to the whole catalog.
	 */
	private final List<CatalogItem<?>> operands;

	Operation(Operator operator, Collection<CatalogItem<?>> operands) {
		this.operator = operator;
		this.operands = new ArrayList<>(operands);
	}

	Operator getOperator() {
		return operator;
	}

	List<CatalogItem<?>> getOperands() {
		return Collections.unmodifiableList(operands);
	}
}
