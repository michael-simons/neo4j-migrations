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
package ac.simons.neo4j.migrations.maven;

import static org.junit.Assert.*;

import ac.simons.neo4j.migrations.core.MigrationsConfig.TransactionMode;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Michael J. Simons
 */
public class AbstractConnectedMojoTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Test
	public void defaultValuesShouldBeCorrect() throws Exception {

		File pom = new File("target/test-classes/project-to-test/");
		assertNotNull(pom);
		assertTrue(pom.exists());

		InfoMojo infoMojo = (InfoMojo) rule.lookupConfiguredMojo(pom, "info");
		assertNotNull(infoMojo);

		URI address = (URI) rule.getVariableValueFromObject(infoMojo, "address");
		assertEquals(address, URI.create("bolt://localhost:7687"));

		String user = (String) rule.getVariableValueFromObject(infoMojo, "user");
		assertEquals(user, "neo4j");

		String password = (String) rule.getVariableValueFromObject(infoMojo, "password");
		assertNull(password);

		String[] packagesToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "packagesToScan");
		assertArrayEquals(new String[0], packagesToScan);

		String[] locationsToScan = (String[]) rule.getVariableValueFromObject(infoMojo, "locationsToScan");
		assertTrue(Pattern.compile("file://.*/neo4j/migrations").matcher(locationsToScan[0]).matches());

		TransactionMode transactionMode = (TransactionMode) rule
			.getVariableValueFromObject(infoMojo, "transactionMode");
		assertEquals(transactionMode, TransactionMode.PER_MIGRATION);

		String database = (String) rule.getVariableValueFromObject(infoMojo, "database");
		assertNull(database);

		boolean verbose = (boolean) rule.getVariableValueFromObject(infoMojo, "verbose");
		assertFalse(verbose);
	}
}

