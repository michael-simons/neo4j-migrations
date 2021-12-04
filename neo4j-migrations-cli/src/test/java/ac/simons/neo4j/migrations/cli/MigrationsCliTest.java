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
package ac.simons.neo4j.migrations.cli;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine;

import java.lang.reflect.Field;

import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.neo4j.driver.Config;

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

			assertThatExceptionOfType(UnsupportedConfigException.class).isThrownBy(cli::getConfig)
				.withMessage(
					"Java based migrations are not supported in native binaries. Please use the Java based distribution.");
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

		assertThatIllegalArgumentException().isThrownBy(() -> cli.getConfig().getOptionalSchemaDatabase())
			.withMessage("You must at least allow 2 connections in the pool to use a separate database.");
	}

	@Test
	void shouldHandleUnsupportedConfigException() throws Exception {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionSupport.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
			HierarchyTraversalMode.TOP_DOWN).get(0);
		field.setAccessible(true);
		field.set(cli, new String[] { "foo.bar" });

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
}
