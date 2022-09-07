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
package ac.simons.neo4j.migrations.annotations.proc.ogm_invalid;

import ac.simons.neo4j.migrations.annotations.proc.ogm.Entity;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * Verbatim copy from Neo4j-OGM.
 *
 * @author Frantisek Hartman
 * @author Michael J. Simons
 */
@RelationshipEntity
public class RelPropertyExistenceConstraintEntity {

	Long id;

	@StartNode
	Entity start;

	@EndNode
	Entity end;

	@Index(unique = true)
	String description;
}
