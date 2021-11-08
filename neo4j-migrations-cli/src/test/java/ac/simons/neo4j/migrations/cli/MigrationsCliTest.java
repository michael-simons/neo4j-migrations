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

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine;

import java.lang.reflect.Field;

import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 */
class MigrationsCliTest {

	@Test
	void shouldFailToScanPackageInNative() throws Exception {

		restoreSystemProperties(() -> {
			System.setProperty(ImageInfo.PROPERTY_IMAGE_CODE_KEY, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
			MigrationsCli cli = new MigrationsCli();
			Field field = ReflectionUtils.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
				ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
			ReflectionUtils.makeAccessible(field);
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
		Field field = ReflectionUtils.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
			ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
		ReflectionUtils.makeAccessible(field);
		field.set(cli, new String[] { "foo.bar" });

		assertThat(cli.getConfig()).isNotNull();
	}

	@Test
	void shouldHandleUnsupportedConfigException() throws Exception {

		MigrationsCli cli = new MigrationsCli();
		Field field = ReflectionUtils.findFields(MigrationsCli.class, f -> "packagesToScan".equals(f.getName()),
			ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
		ReflectionUtils.makeAccessible(field);
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
}
