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
package ac.simons.neo4j.migrations.quarkus;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("squid:S5786") // Quarkus can't work with a package private test when using QuarkusDevModeTest
public class DevConsoleEndpointIT {

	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
	public static final String DEV_TOOLS_ENDPOINT = "/q/dev/eu.michael-simons.neo4j.neo4j-migrations-quarkus/migrations";

	/**
	 * Helper resource returning the number of migrations applied.
	 */
	@Path("/cnt")
	public static class CntResource {

		@Inject
		Driver driver;

		@GET
		public long getCnt() {
			try (var session = driver.session()) {
				return session.run("MATCH (n:__Neo4jMigration) RETURN count(n) AS cnt").single().get("cnt").asLong();
			}
		}
	}

	@RegisterExtension
	static QuarkusDevModeTest test = new QuarkusDevModeTest()
		.withApplicationRoot(archive -> {
				archive.addClass(CntResource.class)
					.addAsResource("neo4j/migrations");
			}
		);

	@Test
	void handlerShouldWork() {

		RestAssured
			.when().get("/cnt")
			.prettyPeek()
			.then().statusCode(200).body(Matchers.is("2"));

		RestAssured
			.given()
			.contentType(FORM_CONTENT_TYPE)
			.formParam("operation", "clean")
			.when().post(DEV_TOOLS_ENDPOINT)
			.prettyPeek()
			.then().statusCode(200);

		RestAssured
			.when().get("/cnt")
			.prettyPeek()
			.then().statusCode(200).body(Matchers.is("0"));
	}

	@Test
	void shouldFailOnInvalidOperation() {

		RestAssured
			.given()
			.contentType(FORM_CONTENT_TYPE)
			.formParam("operation", "fooo")
			.when().post(DEV_TOOLS_ENDPOINT)
			.then().statusCode(500)
			.body("details",
				Matchers.containsString("java.lang.UnsupportedOperationException: fooo"));
	}
}
