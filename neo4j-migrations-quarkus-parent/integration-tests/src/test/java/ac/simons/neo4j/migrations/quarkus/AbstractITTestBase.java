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
package ac.simons.neo4j.migrations.quarkus;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import ac.simons.neo4j.migrations.core.MigrationState;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Mainly for cleaning up the database after the test of a real integration test. Uses the
 * credentials from the environment without any checks if they are defined or not. Things
 * will blow up if you don't define them. Sadly enough,
 * {@link io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback} and friends
 * won't work in a {@link io.quarkus.test.junit.QuarkusIntegrationTest}. However, the
 * classpath scanning doesn't work reliable with
 * {@link io.quarkus.test.junit.QuarkusTest}, as the migrations' library will already be
 * loaded while the implementing class will be loaded with a different class loader, so
 * {@link io.quarkus.test.junit.QuarkusIntegrationTest} must be used on subclasses of this
 * base test.
 *
 * @author Michael J. Simons
 */
abstract class AbstractITTestBase {

	@AfterAll
	static void cleanDatabase() {

		var driver = GraphDatabase.driver(System.getenv("QUARKUS_NEO4J_URI"),
				AuthTokens.basic(System.getenv("QUARKUS_NEO4J_AUTHENTICATION_USERNAME"),
						System.getenv("QUARKUS_NEO4J_AUTHENTICATION_PASSWORD")));
		try (var session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
		}
	}

	void assertStateOfMigrations(MigrationState expectedState) {

		var expected = Stream
			.of("0001: 2nd location", "0002: SomethingJava", "0003: SomethingStatic", "0004: Create constraints",
					"0005: Whatever")
			.map(s -> s + " (" + expectedState + ")")
			.collect(Collectors.joining("\",\"", "[\"", "\"]"));

		RestAssured.given().given().get("/migrations").then().statusCode(200).body(is(equalTo(expected)));
	}

}
