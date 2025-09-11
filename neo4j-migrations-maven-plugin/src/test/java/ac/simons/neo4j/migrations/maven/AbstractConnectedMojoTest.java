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
package ac.simons.neo4j.migrations.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.logging.ConsoleLogging;

import ac.simons.neo4j.migrations.core.Migration;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

/**
 * @author Michael J. Simons
 */
public class AbstractConnectedMojoTest {

	@Rule
	public MojoRule rule = new MojoRule();

	private static String origUserName;

	@BeforeClass
	public static void setFixedSysUser() {
		origUserName = System.getProperty("user.name");
		System.setProperty("user.name", "testor");
	}

	@AfterClass
	public static void restoreSysUSer() {
		if (origUserName != null && !origUserName.trim().isEmpty()) {
			System.setProperty("user.name", origUserName);
		}
	}

	@Test
	public void defaultValuesShouldBeCorrect() throws Exception {

		File pom = new File("target/test-classes/project-to-test/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();

		URI address = (URI) rule.getVariableValueFromObject(infoMojo, "address");
		assertThat(address).isEqualTo(URI.create("bolt://localhost:7687"));

		String user = (String) rule.getVariableValueFromObject(infoMojo, "user");
		assertThat(user).isEqualTo("neo4j");

		String password = (String) rule.getVariableValueFromObject(infoMojo, "password");
		assertThat(password).isNull();

		String[] packagesToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "packagesToScan");
		assertThat(packagesToScan).isEqualTo(new String[0]);

		String[] locationsToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "locationsToScan");
		Pattern expectedLocationsToScan = Pattern.compile("file:///?.*[\\\\/]target[\\\\/]classes/neo4j/migrations/?");
		assertThat(expectedLocationsToScan.matcher(locationsToScan[0]).matches()).isTrue();

		TransactionMode transactionMode = (TransactionMode) rule
			.getVariableValueFromObject(infoMojo, "transactionMode");
		assertThat(transactionMode).isEqualTo(TransactionMode.PER_MIGRATION);

		String database = (String) rule.getVariableValueFromObject(infoMojo, "database");
		assertThat(database).isNull();

		boolean verbose = (boolean) rule.getVariableValueFromObject(infoMojo, "verbose");
		assertThat(verbose).isFalse();

		MigrationsConfig config = infoMojo.getConfig();
		assertThat(config).isNotNull();
		assertThat(config.getOptionalDatabase()).isEmpty();
		assertThat(config.getOptionalInstalledBy()).isEqualTo(Optional.of("testor"));
		assertThat(config.getLocationsToScan()).hasSize(1);
		assertThat(expectedLocationsToScan.matcher(config.getLocationsToScan()[0]).matches()).isTrue();
		assertThat(config.getTransactionMode()).isEqualTo(TransactionMode.PER_MIGRATION);
		assertThat(config.getTransactionTimeout()).isEqualTo(Duration.ofMinutes(1).plusSeconds(23));

		Method getMigrations = Migrations.class.getDeclaredMethod("getMigrations");
		getMigrations.setAccessible(true);
		Migrations migrations = new Migrations(config, Mockito.mock(Driver.class));
		@SuppressWarnings("unchecked") List<Migration> migrationsList = (List<Migration>) getMigrations.invoke(migrations);
		assertThat(migrationsList).hasSize(1).element(0).extracting(Migration::getSource).isEqualTo("V010__Foo.cypher");
	}


	@Test
	public void shouldConfigureImpersonatedUser() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getOptionalImpersonatedUser()).isEqualTo(Optional.of("someoneElse"));
		assertThat(infoMojo.getConfig().getLocationsToScan()[0]).isEqualTo("classpath:/wontwork");
	}

	@Test
	public void shouldConfigureSchemaDatabase() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getOptionalSchemaDatabase()).isEqualTo(Optional.of("anotherDatabase"));
	}

	@Test
	public void createDriverConfigShouldSetCorrectValues() {

		Config config = AbstractConnectedMojo.createDriverConfig();
		assertThat(config.logging()).isInstanceOf(ConsoleLogging.class);
		assertThat(config.userAgent()).startsWith("neo4j-migrations/");
	}

	@Test // GH-1213
	public void outOfOrderShouldNotBeAllowedByDefault() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().isOutOfOrder()).isFalse();
	}

	@Test // GH-1213
	public void outOfOrderShouldBeConfigurable() throws Exception {

		File pom = new File("target/test-classes/out-of-order/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().isOutOfOrder()).isTrue();
	}

	@Test
	public void useFlywayCompatibleChecksumsShouldBeDisabled() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().isUseFlywayCompatibleChecksums()).isFalse();
	}

	@Test
	public void useFlywayCompatibleChecksumsShouldBeEnabled() throws Exception {

		File pom = new File("target/test-classes/out-of-order/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().isUseFlywayCompatibleChecksums()).isTrue();
	}

	@Test // GH-1536
	public void targetShouldBeNullByDefault() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getTarget()).isNull();
	}

	@Test // GH-1536
	public void targetShouldBeApplied() throws Exception {

		File pom = new File("target/test-classes/target/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getTarget()).isEqualTo("next");
	}

	@Test
	public void cypherVersionShouldHaveDefault() throws Exception {

		File pom = new File("target/test-classes/with-imp-and-schema/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.DATABASE_DEFAULT);
	}

	@Test
	public void cypherVersionShouldBeApplied() throws Exception {

		File pom = new File("target/test-classes/cypher-version/");
		assertThat(pom)
			.isNotNull()
			.exists();

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertThat(infoMojo).isNotNull();
		assertThat(infoMojo.getConfig().getCypherVersion()).isEqualTo(MigrationsConfig.CypherVersion.CYPHER_25);
	}
}

