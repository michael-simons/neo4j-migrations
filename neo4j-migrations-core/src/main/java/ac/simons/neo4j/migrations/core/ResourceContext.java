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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;

/**
 * A context in which a resource with a given URL was discovered.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class ResourceContext {

	private final URL url;

	private final MigrationsConfig config;

	private ResourceContext(URL url, MigrationsConfig config) {
		this.url = url;
		this.config = config;
	}

	/**
	 * Static factory method, might be helpful during tests of additional providers.
	 * @param url the url of the given resource
	 * @param config the config used during discovery
	 * @return a new resource context
	 */
	public static ResourceContext of(URL url, MigrationsConfig config) {
		return new ResourceContext(url, config);
	}

	static ResourceContext of(URL url) {
		return ResourceContext.of(url, MigrationsConfig.builder().withAutocrlf(Defaults.AUTOCRLF).build());
	}

	/**
	 * Helper method to extract an optional last element from a URL path. If no such
	 * element exists, returns the full path.
	 * @param url the url containing a path
	 * @return the last path element or if such an element does not exist, the full path
	 */
	static String generateIdentifierOf(URL url) {
		var path = URLDecoder.decode(url.getPath(), Defaults.CYPHER_SCRIPT_ENCODING);
		var lastIndexOf = path.lastIndexOf("/");
		return (lastIndexOf < 0) ? path : path.substring(lastIndexOf + 1);
	}

	/**
	 * {@return the resources URL}
	 */
	public URL getUrl() {
		return this.url;
	}

	/**
	 * Returns the identifier of the underlying resource.
	 * @return the identifier of the underlying resource
	 * @since 1.8.0
	 */
	public String getIdentifier() {
		return generateIdentifierOf(this.url);
	}

	/**
	 * {@return the configuration with which the resource has been discovered}
	 */
	public MigrationsConfig getConfig() {
		return this.config;
	}

	/**
	 * This method tries to get an {@link InputStream} from a {@link URL}. If this URL
	 * points to something on the classpath, it tries to handle the changes introduced in
	 * Spring Boot 3.2.0 about how nested JARs are handle, see <a href=
	 * "https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes#nested-jar-support">Nested
	 * Jar Support</a>. This affects Cypher as well as XML based resources.
	 * @return an {@link InputStream}. The caller is supposed to properly close the
	 * stream.
	 * @since 2.9.2
	 */
	public InputStream openStream() {
		try {
			return this.url.openStream();
		}
		catch (IOException ex) {
			var oldUrl = this.url.toString();
			if (ex instanceof FileNotFoundException && oldUrl.contains("jar:file") && oldUrl.contains("!/BOOT-INF/")) {
				var newUrl = oldUrl.replace("jar:file", "jar:nested").replace("!/BOOT-INF/", "/!BOOT-INF/");
				Migrations.LOGGER.log(Level.FINE,
						"Probably on Spring Boot >= 3.2.0 with new Jar loader, replacing {0} with {1}",
						new Object[] { oldUrl, newUrl });
				try {
					return URI.create(newUrl).toURL().openStream();
				}
				catch (IOException inner) {
					throw new UncheckedIOException(inner);
				}
			}
			throw new UncheckedIOException(ex);
		}
	}

}
