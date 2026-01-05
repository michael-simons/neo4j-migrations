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
package ac.simons.neo4j.migrations.maven;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@MojoTest
public class AbstractConnectedMojoTests {

	private static String origUserName;

	@BeforeAll
	public static void setFixedSysUser() {
		origUserName = System.getProperty("user.name");
		System.setProperty("user.name", "testor");
	}

	@AfterAll
	public static void restoreSysUSer() {
		if (origUserName != null && !origUserName.trim().isEmpty()) {
			System.setProperty("user.name", origUserName);
		}
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/project-to-test/pom.xml")
	public void defaultValuesShouldBeCorrect(InfoMojo infoMojo) throws Exception {

		URI address = (URI) MojoExtension.getVariableValueFromObject(infoMojo, "address");
		assertThat(address).isEqualTo(URI.create("bolt://localhost:7687"));

		String user = (String) MojoExtension.getVariableValueFromObject(infoMojo, "user");
		assertThat(user).isEqualTo("neo4j");

		String password = (String) MojoExtension.getVariableValueFromObject(infoMojo, "password");
		assertThat(password).isNull();

		String[] packagesToScan = (String[]) MojoExtension.getVariableValueFromObject(infoMojo, "packagesToScan");
		assertThat(packagesToScan).isEqualTo(new String[0]);

		String[] locationsToScan = (String[]) MojoExtension.getVariableValueFromObject(infoMojo, "locationsToScan");
		Pattern expectedLocationsToScan = Pattern.compile(
				"file://(/?.*[\\\\/]target[\\\\/]classes|\\$\\{project.build.outputDirectory})/neo4j/migrations/?");
		assertThat(expectedLocationsToScan.matcher(locationsToScan[0]).matches()).isTrue();

		TransactionMode transactionMode = (TransactionMode) MojoExtension.getVariableValueFromObject(infoMojo,
				"transactionMode");
		assertThat(transactionMode).isEqualTo(TransactionMode.PER_MIGRATION);

		String database = (String) MojoExtension.getVariableValueFromObject(infoMojo, "database");
		assertThat(database).isNull();

		boolean verbose = (boolean) MojoExtension.getVariableValueFromObject(infoMojo, "verbose");
		assertThat(verbose).isFalse();

		MigrationsConfig config = infoMojo.getConfig();
		assertThat(config).isNotNull();
		assertThat(config.getOptionalDatabase()).isEmpty();
		assertThat(config.getOptionalInstalledBy()).isEqualTo(Optional.of("testor"));
		assertThat(config.getLocationsToScan()).hasSize(1);
		assertThat(expectedLocationsToScan.matcher(config.getLocationsToScan()[0]).matches()).isTrue();
		assertThat(config.getTransactionMode()).isEqualTo(TransactionMode.PER_MIGRATION);
		assertThat(config.getTransactionTimeout()).isEqualTo(Duration.ofMinutes(1).plusSeconds(23));
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void shouldConfigureImpersonatedUser(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getOptionalImpersonatedUser()).isEqualTo(Optional.of("someoneElse"));
		assertThat(infoMojo.getConfig().getLocationsToScan()[0]).isEqualTo("classpath:/wontwork");
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void shouldConfigureSchemaDatabase(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getOptionalSchemaDatabase()).isEqualTo(Optional.of("anotherDatabase"));
	}

	@Test
	public void createDriverConfigShouldSetCorrectValues() {

		Config config = AbstractConnectedMojo.createDriverConfig();
		assertThat(config.userAgent()).startsWith("neo4j-migrations/");
	}

	@Test // GH-1213
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void outOfOrderShouldNotBeAllowedByDefault(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().isOutOfOrder()).isFalse();
	}

	@Test // GH-1213
	@InjectMojo(goal = "info", pom = "target/test-classes/out-of-order/pom.xml")
	public void outOfOrderShouldBeConfigurable(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().isOutOfOrder()).isTrue();
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void useFlywayCompatibleChecksumsShouldBeDisabled(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().isUseFlywayCompatibleChecksums()).isFalse();
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/out-of-order/pom.xml")
	public void useFlywayCompatibleChecksumsShouldBeEnabled(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().isUseFlywayCompatibleChecksums()).isTrue();
	}

	@Test // GH-1536
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void targetShouldBeNullByDefault(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getTarget()).isNull();
	}

	@Test // GH-1536
	@InjectMojo(goal = "info", pom = "target/test-classes/target/pom.xml")
	public void targetShouldBeApplied(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getTarget()).isEqualTo("next");
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/with-imp-and-schema/pom.xml")
	public void cypherVersionShouldHaveDefault(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.DATABASE_DEFAULT);
	}

	@Test
	@InjectMojo(goal = "info", pom = "target/test-classes/cypher-version/pom.xml")
	public void cypherVersionShouldBeApplied(InfoMojo infoMojo) {

		assertThat(infoMojo.getConfig().getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.CYPHER_25);
	}

}
