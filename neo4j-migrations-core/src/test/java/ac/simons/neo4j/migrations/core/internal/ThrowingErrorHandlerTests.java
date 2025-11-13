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
package ac.simons.neo4j.migrations.core.internal;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Michael J. Simons
 */
class ThrowingErrorHandlerTests {

	@Test
	void warningShouldRethrow() {

		var handler = new ThrowingErrorHandler();
		var ex = new SAXParseException("oops", null);
		assertThatExceptionOfType(SAXParseException.class).isThrownBy(() -> handler.warning(ex)).isEqualTo(ex);
	}

	@Test
	void errorShouldRethrow() {

		var handler = new ThrowingErrorHandler();
		var ex = new SAXParseException("oops", null);
		assertThatExceptionOfType(SAXParseException.class).isThrownBy(() -> handler.error(ex)).isEqualTo(ex);
	}

	@Test
	void errorShouldNotRethrowCvcId1() {

		var handler = new ThrowingErrorHandler();
		var ex = new SAXParseException("cvc-id.1", null);
		assertThatNoException().isThrownBy(() -> handler.error(ex));
	}

	@Test
	void fatalShouldRethrow() {

		var handler = new ThrowingErrorHandler();
		var ex = new SAXParseException("oops", null);
		assertThatExceptionOfType(SAXParseException.class).isThrownBy(() -> handler.fatalError(ex)).isEqualTo(ex);
	}

}
