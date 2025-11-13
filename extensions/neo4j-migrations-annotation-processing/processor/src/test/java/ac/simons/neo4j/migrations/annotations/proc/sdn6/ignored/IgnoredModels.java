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
package ac.simons.neo4j.migrations.annotations.proc.sdn6.ignored;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Models that are ignored because they have no id or only internally generated ids.
 *
 * @author Michael J. Simons
 */
public final class IgnoredModels {

	private IgnoredModels() {
	}

	@Node
	interface Whatever {

	}

	@Node
	static class NoId {

	}

	@Node
	static class InternalId {

		@Id
		@GeneratedValue
		long id;

	}

	@Node
	abstract static class IdButAbstract {

		@Id
		String id;

	}

	static class Someclass {

	}

	@Node
	static class NoIdInHierarchy extends Someclass implements Whatever {

	}

}
