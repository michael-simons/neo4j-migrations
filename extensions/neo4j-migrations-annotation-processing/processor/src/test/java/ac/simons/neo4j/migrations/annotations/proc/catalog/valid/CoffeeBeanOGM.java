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
package ac.simons.neo4j.migrations.annotations.proc.catalog.valid;

import java.util.UUID;

import ac.simons.neo4j.migrations.annotations.catalog.Index;
import ac.simons.neo4j.migrations.annotations.catalog.Required;
import ac.simons.neo4j.migrations.annotations.catalog.Unique;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

/**
 * @author Michael J. Simons
 */
@NodeEntity("CBOGM")
@Unique(properties = { "a", "b", "c" })
@Index(properties = { "a", "b" })
public class CoffeeBeanOGM {

	@Unique
	public UUID uuid;

	@Required
	public String name;

	@Index(indexType = Index.Type.TEXT)
	public String text;

	@Index(indexType = Index.Type.FULLTEXT,
			options = @Index.Option(key = "indexConfig", value = "+{ `fulltext.analyzer`:\"whitespace\" }"))
	public String textB;

	@Required(property = "theName")
	public String nameA;

	@Property(name = "theOtherName")
	@Required
	public String nameB;

	@Required(property = "name")
	public String nameC;

	public String a;

	public String b;

	public String d;

}
