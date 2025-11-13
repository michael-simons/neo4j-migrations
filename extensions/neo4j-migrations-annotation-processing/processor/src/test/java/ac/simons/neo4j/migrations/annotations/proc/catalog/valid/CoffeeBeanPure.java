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
package ac.simons.neo4j.migrations.annotations.proc.catalog.valid;

import java.util.UUID;

import ac.simons.neo4j.migrations.annotations.catalog.Required;
import ac.simons.neo4j.migrations.annotations.catalog.Unique;

/**
 * I did watch a Sebastian Daschner talk while writing this ;)
 *
 * @author Michael J. Simons
 */
@Unique(properties = { "a", "b", "c" })
public class CoffeeBeanPure {

	@Unique
	public UUID uuid;

	@Required
	public String name;

	@Required(property = "theName")
	public String nameA;

	@Required
	public String nameB;

	@Required(property = "name")
	public String nameC;

	public String a;

	public String b;

	public String d;

}
