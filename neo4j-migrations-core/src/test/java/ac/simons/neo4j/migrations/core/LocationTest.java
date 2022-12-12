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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Field;

import ac.simons.neo4j.migrations.core.Location.LocationType;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class LocationTest {

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

		description = Location.of("file:/./foobar");
		assertThat(description.getType()).isEqualTo(LocationType.FILESYSTEM);
		assertThat(description.getName()).isEqualTo("/./foobar");
	}

	@Test
	void withInvalidName() {

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of(":"))
			.withMessage("Invalid resource name: ':'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of(":c"))
			.withMessage("Invalid resource name: ':c'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("c:"))
			.withMessage("Invalid resource name: 'c:'");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("classpath:"))
			.withMessage("Invalid name; a valid file URI must begin with either file:/path (no hostname), file:///path (empty hostname), or file://hostname/path");
		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("file:asd"))
			.withMessage("Invalid name; a valid file URI must begin with either file:/path (no hostname), file:///path (empty hostname), or file://hostname/path");
	}

	@Test
	void withInvalidPrefix() {

		assertThatExceptionOfType(MigrationsException.class).isThrownBy(() -> Location.of("some:thing"))
			.withMessage("Invalid resource prefix: 'some'");
	}

	@Test
	void makeSonarHappy() throws NoSuchFieldException, IllegalAccessException {
		Location relativeFileLocation = Location.of("file:/egal");
		Field field = Location.class.getDeclaredField("name");
		field.setAccessible(true);
		field.set(relativeFileLocation, "egal");
		assertThatIllegalArgumentException().isThrownBy(relativeFileLocation::toUri).withMessage("Relative path in absolute URI: file://egal");
	}
}
