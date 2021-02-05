/*
 * Copyright 2020-2021 the original author or authors.
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
package ac.simons.neo4j.migrations.cli;

import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Version provider modelled after the official example
 * <a href="https://github.com/remkop/picocli/blob/master/picocli-examples/src/main/java/picocli/examples/VersionProviderDemo2.java">here</a>
 *
 * @author Michael J. Simons
 * @soundtrack Phil Collins - ...But Seriously
 * @since 0.0.5
 */
final class ManifestVersionProvider implements IVersionProvider {

	@Override
	public String[] getVersion() throws Exception {
		Enumeration<URL> resources = MigrationsCli.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			try {
				Manifest manifest = new Manifest(url.openStream());
				if (isApplicableManifest(manifest)) {
					Attributes attr = manifest.getMainAttributes();
					return new String[] { get(attr, "Implementation-Title") + " v" +
						get(attr, "Implementation-Version") + "" };
				}
			} catch (IOException ex) {
				return new String[] { "Unable to read from " + url + ": " + ex };
			}
		}
		return new String[] { "Unknown version" };
	}

	private static boolean isApplicableManifest(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		return ManifestVersionProvider.class.getPackage().getName()
			.equals(get(attributes, "Automatic-Module-Name"));
	}

	private static Object get(Attributes attributes, String key) {
		return attributes.get(new Attributes.Name(key));
	}

}
