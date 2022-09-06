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
package ac.simons.neo4j.migrations.annotations.proc.ogm_intermediate;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.IdStrategy;

/**
 * @author Michael J. Simons
 */
@NodeEntity(label = "EntityWithExternallyGeneratedId")
public class EntityWithExternallyGeneratedId {

	/**
	 * Non-functional id generation for testing purpose.
	 */
	public static class MyGeneration implements IdStrategy {
		@Override
		public Object generateId(Object entity) {
			return "People tried to put us down…";
		}
	}

	@Id @GeneratedValue(strategy = MyGeneration.class)
	String id;
}
