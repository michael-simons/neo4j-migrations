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
package ac.simons.neo4j.migrations.cli;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
			setPackagesToScan(cli, new String[] { "foo.bar" });

			assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
				.withMessage(
					"Java-based migrations are not supported in native binaries. Please use the Java-based distribution.");
		});
	}

	@Test
	void shouldNotFailToScanPackageInJVM() throws IllegalAccessException {

		MigrationsCli cli = new MigrationsCli();
		setPackagesToScan(cli, new String[] { "foo.bar" });

		assertThat(cli.getConfig()).isNotNull();
	}

	@Test
	void shouldConfigureImpersonatedUser() {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--impersonate", "someoneElse");

		assertThat(cli.getConfig().getOptionalImpersonatedUser()).hasValue("someoneElse");
	}

	@Test
	void shouldConfigureSchemaDatabase() {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--schema-database", "aDatabaseForTheSchema", "--max-connection-pool-size", "2");

		assertThat(cli.getConfig().getOptionalSchemaDatabase()).hasValue("aDatabaseForTheSchema");
	}

	@Test
	void shouldRequire2Connections() {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--schema-database", "aDatabaseForTheSchema", "--max-connection-pool-size", "1");

		assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
			.withMessage("You must at least allow 2 connections in the pool to use a separate database.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "a/path, classpath://my/path, file://file/path", "a/single/path" })
	void shouldThrowExceptionForLocationsToScanIfRunningNativeAndImplicitClasspathIsDefined(String locations)
		throws Exception {
		MigrationsCli cli = new MigrationsCli();
		setLocationsToScan(cli, Arrays.stream(locations.split(",")).map(String::trim).toArray(String[]::new));

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			assertThatIllegalArgumentException().isThrownBy(cli::getConfig)
				.withMessageStartingWith("Classpath based resource locations are not support in native image: ");
		});
	}

	static void setPackagesToScan(MigrationsCli cli, String[] value) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--package", Arrays.stream(value).collect(Collectors.joining(",")));
	}

	static void setLocationsToScan(MigrationsCli cli, String[] value) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--location", Arrays.stream(value).collect(Collectors.joining(",")));
	}

	@Test
	void shouldHandleIllegalArgumentsToConfiguration() throws Exception {

		MigrationsCli cli = new MigrationsCli();
		setPackagesToScan(cli, new String[] { "foo.bar" });

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

	private void setUserName(MigrationsCli cli) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--username", "neo4j");
	}

	private void setPassword(MigrationsCli cli) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--password", "secret");
	}

	private void setPasswordEnv(MigrationsCli cli, String name) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--password:env", name);
	}

	private void setPasswordFile(MigrationsCli cli, File file) {
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--password:file", file.getAbsolutePath());
	}

	@Test
	void createDriverConfigShouldSetCorrectValues() {

		MigrationsCli cli = new MigrationsCli();
		CommandLine commandLine = new CommandLine(cli);
		commandLine.parseArgs("--max-connection-pool-size", "4711");

		Config config = cli.createDriverConfig();
		assertThat(config.maxConnectionPoolSize()).isEqualTo(4711);
		assertThat(config.userAgent()).startsWith("neo4j-migrations/");
	}

	@Nested
	class PropertiesSupport {

		@Test
		void shouldWriteToProperties() {
			MigrationsCli cli = new MigrationsCli();

			CommandLine commandLine = new CommandLine(cli);
			commandLine.parseArgs("--package", "a", "--package", "b", "--password", "1234");
			Properties properties = cli.toProperties();
			assertThat(properties)
				.containsEntry("package", "a,b")
				.containsEntry("password", "1234")
				.doesNotContainKey("location")
				.doesNotContainKey("max-connection-pool-size");
		}

		@Test
		void shouldReadProperties() throws IOException {

			Path nm = Files.createTempFile("nm", ".properties");
			Files.write(nm, "location=a,b,c".getBytes(StandardCharsets.UTF_8));
			Optional<Properties> optionalProperties = MigrationsCli.loadProperties(nm.toAbsolutePath().toString());
			assertThat(optionalProperties).isPresent()
				.hasValueSatisfying(p -> assertThat(p).containsEntry("location", "a,b,c"));
		}
	}

	@Nested
	class LocationsToScan {

		@Test
		void shouldUseConfiguredValues() throws IllegalAccessException {

			MigrationsCli cli = new MigrationsCli();
			setLocationsToScan(cli, new String[] { "a", "b" });

			MigrationsConfig config = cli.getConfig();
			assertThat(config.getLocationsToScan()).containsExactlyInAnyOrder("a", "b");
		}

		@Test
		void shouldNotDefaultWhenPackagesAreSet() throws IllegalAccessException {

			MigrationsCli cli = new MigrationsCli();
			setPackagesToScan(cli, new String[] { "a.b" });

			MigrationsConfig config = cli.getConfig();
			assertThat(config.getLocationsToScan()).isEmpty();
			assertThat(config.getPackagesToScan()).containsExactly("a.b");
		}

		@Test
		void shouldUseDefault() throws IOException {

			MigrationsCli cli = new MigrationsCli();

			Path defaultPath = Paths.get("neo4j/migrations");
			Path newDir = Files.createDirectories(defaultPath);
			try {
				MigrationsConfig config = cli.getConfig();
				assertThat(config.getLocationsToScan()).containsExactly(defaultPath.toUri().toString());
			} finally {
				if (!defaultPath.equals(newDir)) {
					deltree(newDir);
				}
			}
		}
	}

	static void deltree(Path dir) throws IOException {

		Files.walkFileTree(dir.getParent(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
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
		void shouldUseEnvFirst() {

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

		@Test
		void shouldCheckExistenceOfFile() {

			MigrationsCli cli = new MigrationsCli();

			setUserName(cli);
			setPasswordFile(cli, new File("non-existing"));

			assertThatExceptionOfType(CommandLine.ParameterException.class).isThrownBy(cli::getAuthToken)
				.withMessage("Missing required option: '--password', '--password:env' or '--password:file'");
		}

		@Test
		void shouldTrim() {

			MigrationsCli cli = new MigrationsCli();

			setUserName(cli);
			setPasswordEnv(cli, "emptyPassword");

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
