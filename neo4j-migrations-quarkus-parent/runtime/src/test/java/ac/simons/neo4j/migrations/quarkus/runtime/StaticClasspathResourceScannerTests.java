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
package ac.simons.neo4j.migrations.quarkus.runtime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import ac.simons.neo4j.migrations.core.MigrationsException;
import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Order is necessary as the handler doesn't want to go away after first discovery.
 *
 * @author Michael J. Simons
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StaticClasspathResourceScannerTests {

	@Test
	@Order(10)
	void shouldDetectResourcesInNativeImageWithoutHandler() throws Exception {

		var scanner = new StaticClasspathResourceScanner();
		var resource = new ResourceWrapper();
		var locations = List.of("/foo");
		resource.setUrl("file:///foo/bar.cypher");
		resource.setPath("foo");
		scanner.setResources(Set.of(resource));

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> scanner.scan(locations))
				.withCauseInstanceOf(MalformedURLException.class)
				.withStackTraceContaining("unknown protocol: resource");
		});
	}

	@Test
	@Order(20)
	void shouldDetectResourcesInNativeImage() throws Exception {

		var scanner = new StaticClasspathResourceScanner();
		var resource = new ResourceWrapper();
		resource.setUrl("file:///foo/bar.cypher");
		resource.setPath("foo");
		scanner.setResources(Set.of(resource));

		restoreSystemProperties(() -> {
			System.setProperty("java.protocol.handler.pkgs", "ac.simons.neo4j.migrations.quarkus.runtime.protocol");
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			List<URL> urls = scanner.scan(List.of("/foo"));
			assertThat(urls).map(URL::toString).containsExactly("resource:/foo");
		});
	}

	@Test
	@Order(30)
	void shouldStoreResources() {

		var resource = new ResourceWrapper();
		var scanner = new StaticClasspathResourceScanner();
		scanner.setResources(Set.of(resource));
		assertThat(scanner.getResources()).containsExactly(resource);
	}

}
