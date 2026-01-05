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
package ac.simons.neo4j.migrations.core;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utility class to retrieve the version of the core module aka the product version.
 *
 * @author Michael J. Simons
 * @since 1.2.1
 */
final class ProductVersion {

	private static volatile String value;

	private ProductVersion() {
	}

	static String getValue() {

		String computedVersion = value;
		if (computedVersion == null) {
			synchronized (ProductVersion.class) {
				computedVersion = value;
				if (computedVersion == null) {
					value = getVersionImpl();
					computedVersion = value;
				}
			}
		}
		return computedVersion;
	}

	private static String getVersionImpl() {
		try {
			Enumeration<URL> resources = Migrations.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Manifest manifest = new Manifest(url.openStream());
				if (isApplicableManifest(manifest)) {
					Attributes attr = manifest.getMainAttributes();
					return get(attr, "Implementation-Version").toString();
				}
			}
		}
		catch (IOException ex) {
			throw new MigrationsException("Unable to read from neo4j-migrations-core manifest.");
		}

		return "unknown";
	}

	private static boolean isApplicableManifest(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		return "neo4j-migrations".equals(get(attributes, "Artifact-Id"));
	}

	private static Object get(Attributes attributes, String key) {
		return attributes.get(new Attributes.Name(key));
	}

}
