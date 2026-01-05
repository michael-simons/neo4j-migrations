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

import java.lang.reflect.Field;
import java.nio.file.Path;

import ac.simons.neo4j.migrations.core.Location.LocationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class LocationTests {

	@Test
	void fromValidPrefixes() {

		Location description;
		description = Location.of("classpath:foobar");
		assertThat(description.getType()).isEqualTo(LocationType.CLASSPATH);
		assertThat(description.getName()).isEqualTo("/foobar");

		description = Location.of("classPath:foobar");
		assertThat(description.getType()).isEqualTo(LocationType.CLASSPATH);
		assertThat(description.getName()).isEqualTo("/foobar");

		description = Location.of(" classPath : foobar ");
		assertThat(description.getType()).isEqualTo(LocationType.CLASSPATH);
		assertThat(description.getName()).isEqualTo("/foobar");
	}

	@Test
	void relativeFiles() {

		Location description;
		var workingDir = System.getProperty("user.dir");
		description = Location.of("./test/");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo(workingDir + "/test");

		description = Location.of("../test/");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo(Path.of(workingDir).getParent().resolve("test").toString());

		description = Location.of(".././test/.");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo(Path.of(workingDir).getParent().resolve("test").toString());

		description = Location.of("./.././test/..");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo(Path.of(workingDir).getParent().toString());

		description = Location.of("file:/./foobar");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo("/foobar");

		description = Location.of("file://test/./foobar");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo("/foobar");

		description = Location.of("file://test/../foobar");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo("/foobar");

		description = Location.of("file://test/../../foobar");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo("/foobar");
	}

	@Test
	void withInvalidLocation() {

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of(":"))
			.withMessage("Invalid location: ':'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("c:"))
			.withMessage("Invalid location: 'c:'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("classpath:"))
			.withMessage("Invalid location: 'classpath:'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("file:asd"))
			.withMessage(
					"Invalid path; a valid file location must begin with either file:/path (no hostname), file:///path (empty hostname), or file://hostname/path");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("file:"))
			.withMessage("Invalid location: 'file:'");
	}

	@Test
	void withInvalidScheme() {

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of(":c"))
			.withMessage("Invalid scheme: '', supported schemes are 'classpath:', 'file:'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("some:thing"))
			.withMessage("Invalid scheme: 'some', supported schemes are 'classpath:', 'file:'");
	}

	@Test
	void makeSonarHappy() throws NoSuchFieldException, IllegalAccessException {
		Location relativeFileLocation = Location.of("file:/egal");
		Field field = Location.class.getDeclaredField("name");
		field.setAccessible(true);
		field.set(relativeFileLocation, "egal");
		assertThatIllegalArgumentException().isThrownBy(relativeFileLocation::toUri)
			.withMessage("Relative path in absolute URI: file://egal");
	}

}
