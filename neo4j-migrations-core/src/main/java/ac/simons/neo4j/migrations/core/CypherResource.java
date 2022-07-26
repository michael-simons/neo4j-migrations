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
package ac.simons.neo4j.migrations.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;

/**
 * Abstraction over Cypher resources.
 *
 * @author Michael J. Simons
 * @soundtrack Koljah - Aber der Abgrund
 * @since 1.8.0
 */
public interface CypherResource {

	/**
	 * Creates a new resource for the given URL, using the default settings for autocrlf.
	 *
	 * @param url The url to create the resource from
	 * @return a new Cypher resource
	 */
	static CypherResource of(URL url) {
		return of(url, Defaults.AUTOCRLF);
	}

	/**
	 * Creates a new resource for the given URL.
	 *
	 * @param url      The url to create the resource from
	 * @param autocrlf Setting for autocrlf.
	 * @return a new Cypher resource
	 */
	static CypherResource of(URL url, boolean autocrlf) {

		return new DefaultCypherResource(ResourceContext.generateIdentifierOf(url), autocrlf,
			() -> {
				try {
					return url.openStream();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
	}

	/**
	 * Starts building a Cypher resource from the given content
	 *
	 * @param content The string containing cypher
	 * @return first step of building the resource
	 * @since 1.8.0
	 */
	static WithContent withContent(String content) {
		return identifier -> new DefaultCypherResource(identifier, Defaults.AUTOCRLF,
			() -> new ByteArrayInputStream(content.getBytes(Defaults.CYPHER_SCRIPT_ENCODING)));
	}

	/**
	 * Terminal step of building a resource
	 *
	 * @since 1.8.0
	 */
	interface WithContent {

		/**
		 * Adds an identifier to the content and builds the resource.
		 *
		 * @param identifier the identifier to use
		 * @return a new Cypher resource
		 */
		CypherResource identifiedBy(String identifier);
	}

	/**
	 * The identifier for this resource must be parsable into a {@link MigrationVersion}, see {@link MigrationVersion#canParse(String)}.
	 *
	 * @return An identifier for this resource
	 */
	String getIdentifier();

	/**
	 * @return The checksum of this resource
	 */
	String getChecksum();

	/**
	 * Returns all statements that are not single line comments. The list of statements might contain {@code :use database}
	 * statements, which neo4j-migrations (browser and cypher-shell) can deal with but the pure driver can't.
	 * <p>
	 * The returned list will never include single line comments, but the statements themselves might contain comments.
	 *
	 * @return A list of executable statements
	 */
	List<String> getExecutableStatements();

	/**
	 * @return A list of surely identifiable single line comments, either "standalone" or before a valid cypher statement
	 */
	List<String> getSingleLineComments();
}
