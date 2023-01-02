/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.formats.csv.test_migrations;

// tag::content[]
import java.net.URI;

import org.neo4j.driver.Query;

import ac.simons.neo4j.migrations.formats.csv.AbstractLoadCSVMigration;

// end::content[]
/**
 * @author Michael J. Simons
 */
// tag::content[]
public class R050__LoadBookData extends AbstractLoadCSVMigration {

	public R050__LoadBookData() {
		super(URI.create("https://raw.githubusercontent.com/michael-simons/goodreads/master/all.csv"), true);
	}

	@Override
	public Query getQuery() {
		// language=cypher
		return new Query("""
			LOAD CSV FROM '%s' AS row FIELDTERMINATOR ';'
			MERGE (b:Book {title: trim(row[1])})
			SET b.type = row[2], b.state = row[3]
			WITH b, row
			UNWIND split(row[0], '&') AS author
			WITH b, split(author, ',') AS author
			WITH b, ((trim(coalesce(author[1], '')) + ' ') + trim(author[0])) AS author
			MERGE (a:Person {name: trim(author)})
			MERGE (a)-[r:WROTE]->(b)
			WITH b, a
			WITH b, collect(a) AS authors
			RETURN b.title, b.state, authors
			""");
	}
}
// end::content[]
