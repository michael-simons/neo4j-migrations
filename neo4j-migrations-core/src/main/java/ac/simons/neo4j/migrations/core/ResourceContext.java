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
package ac.simons.neo4j.migrations.core;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * A context in which a resource with a given URL was discovered.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class ResourceContext {

	/**
	 * Static factory method, might be helpful during tests of additional providers
	 *
	 * @param url    The url of the given resource
	 * @param config The config used during discovery
	 * @return A new resource context
	 */
	public static ResourceContext of(URL url, MigrationsConfig config) {
		return new ResourceContext(url, config);
	}

	private final URL url;

	private final MigrationsConfig config;

	private ResourceContext(URL url, MigrationsConfig config) {
		this.url = url;
		this.config = config;
	}

	/**
	 * @return the resources {@link URL}
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * @return The identifier of the underlying resource
	 * @since 1.8.0
	 */
	public String getIdentifier() {
		return generateIdentifierOf(url);
	}

	/**
	 * Helper method to extract an optional last element from a URL path. If no such element exists, returns the full path.
	 *
	 * @param url The url containing a path
	 * @return The last path element or if such an element does not exists, the full path
	 */
	static String generateIdentifierOf(URL url) {
		String path = url.getPath();
		try {
			path = URLDecoder.decode(path, Defaults.CYPHER_SCRIPT_ENCODING.name());
		} catch (UnsupportedEncodingException e) {
			throw new MigrationsException("Somethings broken: UTF-8 encoding not supported.");
		}
		int lastIndexOf = path.lastIndexOf("/");
		return lastIndexOf < 0 ? path : path.substring(lastIndexOf + 1);
	}

	/**
	 * @return the configuration with which the resource has been discovered
	 */
	public MigrationsConfig getConfig() {
		return config;
	}
}
