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
package ac.simons.neo4j.migrations.cli;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.security.InternalAuthToken;

/**
 * @author Michael J. Simons
 */
class MigrationsCliTest {

	@Test
	void shouldFailToScanPackageInNative() throws Exception {

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			MigrationsCli cli = new MigrationsCli();
			Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
				HierarchyTraversalMode.TOP_DOWN).get(0);
			field.setAccessible(true);
			try {
				field.set(cli, new String[] { "foo.bar" });
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
				.withMessage(
					"Java-based migrations are not supported in native binaries. Please use the Java-based distribution.");
		});
	}

	@Test
	void shouldNotFailToScanPackageInJVM() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, new String[] { "foo.bar" });

		assertThat(cli.getConfig()).isNotNull();
	}

	@Test
	void shouldConfigureImpersonatedUser() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "impersonatedUser".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, "someoneElse");

		assertThat(cli.getConfig().getOptionalImpersonatedUser()).hasValue("someoneElse");
	}

	@Test
	void shouldConfigureSchemaDatabase() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "schemaDatabase".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, "aDatabaseForTheSchema");

		field = ReflectionSupport.findFields(MigrationsCli.class, f -> "maxConnectionPoolSize".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, 2);

		assertThat(cli.getConfig().getOptionalSchemaDatabase()).hasValue("aDatabaseForTheSchema");
	}

	@Test
	void shouldRequire2Connections() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "schemaDatabase".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, "aDatabaseForTheSchema");

		field = ReflectionSupport.findFields(MigrationsCli.class, f -> "maxConnectionPoolSize".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, 1);

		assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
			.withMessage("You must at least allow 2 connections in the pool to use a separate database.");
	}

	@Test
	void shouldThrowExceptionForLocationsToScanIfRunningNativeAndImplicitClasspathIsDefined() throws Exception {
		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "locationsToScan".equals(f.getName()),
				HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, new String[] { "a/path", "classpath://my/path", "file://file/path" });

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
					.withMessage("Implicit classpath resource locations are not support in native image: a/path, classpath://my/path");
		});
	}

	@Test
	void shouldHandleIllegalArgumentsToConfiguration() throws Exception {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
			HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, new String[] { "foo.bar" });

		setUserName(cli);
		setPassword(cli);

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			ConnectedCommand cmd = new ConnectedCommand() {

				@Override
				MigrationsCli getParent() {
					return cli;
				}

				@Override
				Integer withMigrations(Migrations migrations) {
					return null;
				}
			};
			assertThat(cmd.call()).isEqualTo(CommandLine.ExitCode.USAGE);
		});
	}

	private void setUserName(MigrationsCli cli) throws IllegalAccessException {
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "user".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, "neo4j");
	}

	private void setPassword(MigrationsCli cli) throws IllegalAccessException {
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "password".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, "secret".toCharArray());
	}

	private void setPasswordEnv(MigrationsCli cli, String name) throws IllegalAccessException {
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "passwordEnv".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, name);
	}

	private void setPasswordFile(MigrationsCli cli, File file) throws IllegalAccessException {
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "passwordFile".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, file);
	}

	@Test
	void createDriverConfigShouldSetCorrectValues() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();

		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "maxConnectionPoolSize".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, 4711);

		Config config = cli.createDriverConfig();
		assertThat(config.maxConnectionPoolSize()).isEqualTo(4711);
		assertThat(config.userAgent()).startsWith("neo4j-migrations/");
	}

	@Nested
	class PasswordOptions {

		@Test
		void shouldUsePasswordFirst() throws Exception {

			MigrationsCli cli = new MigrationsCli();
			setUserName(cli);
			setPassword(cli);
			setPasswordEnv(cli, "superSecretSuperPassword");
			setPasswordFile(cli, new File("X"));

			AuthToken authToken = cli.getAuthToken();
			assertAuthToken(authToken, "secret");
		}

		@Test
		void shouldUseEnvFirst() throws Exception {

			MigrationsCli cli = new MigrationsCli();
			setUserName(cli);
			setPasswordEnv(cli, "superSecretSuperPassword");
			setPasswordFile(cli, new File("X"));

			AuthToken authToken = cli.getAuthToken();
			assertAuthToken(authToken, "Geheim");
		}

		@Test
		void shouldUseFile() throws Exception {

			Path passwordfile = Files.createTempFile("passwordfile", "");
			Files.write(passwordfile, "Vertraulich".getBytes(StandardCharsets.UTF_8));

			MigrationsCli cli = new MigrationsCli();
			setUserName(cli);
			setPasswordFile(cli, passwordfile.toFile());

			AuthToken authToken = cli.getAuthToken();
			assertAuthToken(authToken, "Vertraulich");
		}

		private void setCommandSpec(MigrationsCli cli) throws IllegalAccessException {

			Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "commandSpec".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
			field.setAccessible(true);
			CommandLine.Model.CommandSpec mock = Mockito.mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_MOCKS);
			field.set(cli, mock);
		}

		@Test
		void shouldCheckExistenceOfFile() throws Exception {

			MigrationsCli cli = new MigrationsCli();

			setUserName(cli);
			setPasswordFile(cli, new File("non-existing"));
			setCommandSpec(cli);

			assertThatExceptionOfType(CommandLine.ParameterException.class).isThrownBy(cli::getAuthToken)
				.withMessage("Missing required option: '--password', '--password:env' or '--password:file'");
		}

		@Test
		void shouldTrim() throws Exception {

			MigrationsCli cli = new MigrationsCli();

			setUserName(cli);
			setPasswordEnv(cli, "emptyPassword");
			setCommandSpec(cli);

			assertThatExceptionOfType(CommandLine.ParameterException.class).isThrownBy(cli::getAuthToken)
				.withMessage("Missing required option: '--password', '--password:env' or '--password:file'");
		}

		private void assertAuthToken(AuthToken authToken, String expectedCredentials) {
			assertThat(authToken).isInstanceOf(InternalAuthToken.class);
			assertThat(((InternalAuthToken) authToken).toMap())
				.containsEntry("credentials", Values.value(expectedCredentials));
		}
	}
}
