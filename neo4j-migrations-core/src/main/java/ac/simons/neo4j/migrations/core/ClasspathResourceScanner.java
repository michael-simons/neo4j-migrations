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
package ac.simons.neo4j.migrations.core;

import java.net.URL;
import java.util.List;

/**
 * A scanner for Cypher resources (resources ending with
 * {@link Defaults#CYPHER_SCRIPT_EXTENSION}) available in the classpath.
 * <p>
 * This is an interface that you would not normally implement. It is needed in scenarios
 * where resources must be enumerated upfront (i.e. in a GraalVM native image).
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
public interface ClasspathResourceScanner {

	/**
	 * Scan the given locations for resources matching the criteria of this scanner. The
	 * resources might be filtered later again.
	 * @param locations the locations to scan
	 * @return the resources found
	 */
	List<URL> scan(List<String> locations);

}
