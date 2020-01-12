/*
 * Copyright 2020 the original author or authors.
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
package ac.simons.neo4j.migrations;

import ac.simons.neo4j.migrations.Location.LocationType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class LocationTest {

	@Test
	void fromValidPrefixes() {

		Location description;
		description = Location.of("classpath:foobar");
		Assertions.assertEquals(LocationType.CLASSPATH, description.getType());
		Assertions.assertEquals("foobar", description.getName());

		description = Location.of("classPath:foobar");
		Assertions.assertEquals(LocationType.CLASSPATH, description.getType());
		Assertions.assertEquals("foobar", description.getName());

		description = Location.of(" classPath : foobar ");
		Assertions.assertEquals(LocationType.CLASSPATH, description.getType());
		Assertions.assertEquals("foobar", description.getName());

		description = Location.of("FileSystem:foobar");
		Assertions.assertEquals(LocationType.FILESYSTEM, description.getType());
		Assertions.assertEquals("foobar", description.getName());
	}

	@Test
	void withInvalidName() {

		String msg;
		msg = Assertions.assertThrows(MigrationsException.class, () -> Location.of(":")).getMessage();
		Assertions.assertEquals("Invalid resource name: ':'", msg);
		msg = Assertions.assertThrows(MigrationsException.class, () -> Location.of(":c")).getMessage();
		Assertions.assertEquals("Invalid resource name: ':c'", msg);
		msg = Assertions.assertThrows(MigrationsException.class, () -> Location.of("c:")).getMessage();
		Assertions.assertEquals("Invalid resource name: 'c:'", msg);
		msg = Assertions.assertThrows(MigrationsException.class, () -> Location.of("classpath:"))
			.getMessage();
		Assertions.assertEquals("Invalid name.", msg);
	}

	@Test
	void withInvalidPrefix() {

		String msg;
		msg = Assertions.assertThrows(MigrationsException.class, () -> Location.of("some:thing"))
			.getMessage();
		Assertions.assertEquals("Invalid resource prefix: 'some'", msg);
	}
}
