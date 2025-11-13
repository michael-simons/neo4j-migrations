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
package ac.simons.neo4j.migrations.annotations.proc.ogm;

import org.neo4j.ogm.annotation.CompositeIndex;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Verbatim copy from Neo4j-OGM.
 *
 * @author Frantisek Hartman
 * @author Michael J. Simons
 */
@NodeEntity(label = "EntityWithMultipleCompositeIndexes")
@CompositeIndex({ "firstName", "age" })
@CompositeIndex({ "firstName", "email" })
public class MultipleCompositeIndexEntity {

	Long id;

	String firstName;

	int age;

	String email;

}
