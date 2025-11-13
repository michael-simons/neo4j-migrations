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
package ac.simons.neo4j.migrations.annotations.proc.sdn6.labels;

import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Michael J. Simons
 */
public final class MultipleExplicitLabels {

	private MultipleExplicitLabels() {
	}

	@Node({ "l1", "l2", "l3" })
	static class MultipleValues {

		@org.springframework.data.annotation.Id
		@GeneratedValue(UUIDStringGenerator.class)
		String uuid;

	}

	@Node(primaryLabel = "pl", value = { "l1", "l2", "l3" })
	static class PrimaryAndValuesCombined {

		@Id
		@GeneratedValue(GeneratedValue.UUIDGenerator.class)
		UUID uuid;

	}

}
