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
package ac.simons.neo4j.migrations.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Michael J. Simons
 */
final class StringsTest {

	@ParameterizedTest
	@CsvSource({
		"DAS_IST_EIN_TEST,dasIstEinTest",
		"DAS,das",
		"DAS__AUCH,dasAuch",
		"dasEbenfalls,dasEbenfalls",
		"dasEBenfalls,dasEbenfalls"
	})
	void toCamelCaseShouldWork(String in, String expected) {
		assertThat(Strings.toCamelCase(in)).isEqualTo(expected);
	}
}
