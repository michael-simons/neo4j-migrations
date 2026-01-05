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
package ac.simons.neo4j.migrations.annotations.proc.sdn6.labels;

import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Used for tests only.
 *
 * @author Michael J. Simons
 */
public final class SingleExplicitLabels {

	private SingleExplicitLabels() {
	}

	@Node("1o1")
	static class AsValue {

		@Id
		Long someId;

	}

	@Node(primaryLabel = "pl")
	static class AsPrimaryLabel {

		@Id
		UUID someOtherId;

	}

}
