/*
 * Copyright 2020-2024 the original author or authors.
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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 * @soundtrack Obiymy Doschu - Son
 */
class MigrationsTest {

	@Test
	void logMessageSupplierForCallbacksShouldWorkWithoutDescription() {

		Callback callback = mock(Callback.class);
		when(callback.getOptionalDescription()).thenReturn(Optional.empty());

		Supplier<String> logMessageSupplier = Migrations.logMessageSupplier(callback, LifecyclePhase.BEFORE_CLEAN);
		assertThat(logMessageSupplier.get()).isEqualTo("Invoked beforeClean callback.");
	}

	@Test
	void logMessageSupplierForCallbacksShouldWorkWithDescription() {

		Callback callback = mock(Callback.class);
		when(callback.getOptionalDescription()).thenReturn(Optional.of("Hallo, Welt."));

		Supplier<String> logMessageSupplier = Migrations.logMessageSupplier(callback, LifecyclePhase.BEFORE_CLEAN);
		assertThat(logMessageSupplier.get()).isEqualTo("Invoked \"Hallo, Welt.\" before clean.");
	}

	@Test
	void deletingAVersionShouldRequireAVersion() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), mock(Driver.class));
		assertThatIllegalArgumentException().isThrownBy(() -> migrations.delete(null)).withMessage("A valid version must be passed to the delete operation");
	}
}
