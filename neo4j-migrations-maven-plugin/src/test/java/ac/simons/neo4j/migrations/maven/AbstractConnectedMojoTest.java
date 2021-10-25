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
package ac.simons.neo4j.migrations.maven;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ac.simons.neo4j.migrations.core.MigrationsConfig;
import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

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
		assertNotNull(pom);
		assertTrue(pom.exists());

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertNotNull(infoMojo);

		URI address = (URI) rule.getVariableValueFromObject(infoMojo, "address");
		assertEquals(URI.create("bolt://localhost:7687"), address);

		String user = (String) rule.getVariableValueFromObject(infoMojo, "user");
		assertEquals("neo4j", user);

		String password = (String) rule.getVariableValueFromObject(infoMojo, "password");
		assertNull(password);

		String[] packagesToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "packagesToScan");
		assertArrayEquals(new String[0], packagesToScan);

		String[] locationsToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "locationsToScan");
		Pattern expectedLocationsToScan = Pattern.compile("file://.*/neo4j/migrations");
		assertTrue(expectedLocationsToScan.matcher(locationsToScan[0]).matches());

		TransactionMode transactionMode = (TransactionMode) rule
			.getVariableValueFromObject(infoMojo, "transactionMode");
		assertEquals(TransactionMode.PER_MIGRATION, transactionMode);

		String database = (String) rule.getVariableValueFromObject(infoMojo, "database");
		assertNull(database);

		boolean verbose = (boolean) rule.getVariableValueFromObject(infoMojo, "verbose");
		assertFalse(verbose);

		MigrationsConfig config = infoMojo.getConfig();
		assertNotNull(config);
		assertNull(config.getDatabase());
		assertEquals("testor", config.getInstalledBy());
		assertEquals(1, config.getLocationsToScan().length);
		assertTrue(expectedLocationsToScan.matcher(config.getLocationsToScan()[0]).matches());
		assertEquals(TransactionMode.PER_MIGRATION, config.getTransactionMode());
	}
}

