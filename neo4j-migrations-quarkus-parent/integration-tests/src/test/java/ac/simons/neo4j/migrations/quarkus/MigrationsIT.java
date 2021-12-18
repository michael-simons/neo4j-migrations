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
package ac.simons.neo4j.migrations.quarkus;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The classpath scanning doesn't work reliable with {@link io.quarkus.test.junit.QuarkusTest}, as the migrations library
 * will already be loaded while the implementing class will be loaded with a different class loader
 *
 * @author Michael J. Simons
 */
@QuarkusIntegrationTest
class MigrationsIT {

	@Test
	void cypherAndJavaBasedMigrationsShouldHaveBeenApplied() {

		var expected = Stream.of("0001: 2nd location", "0002: SomethingJava")
			.collect(Collectors.joining("\",\"", "[\"", "\"]"));

		RestAssured.given()
			.when().get("/migrations")
			.then().statusCode(200)
			.body(is(equalTo(expected)));
	}
}
