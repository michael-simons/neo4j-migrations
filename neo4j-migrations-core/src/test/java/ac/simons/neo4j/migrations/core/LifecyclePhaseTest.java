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
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Michael J. Simons
 */
class LifecyclePhaseTest {

	@ParameterizedTest
	@CsvSource(value = {
			"afterMigrate.cypher,n/a,true",
			"beforeMigrate__01test.cypher,01test,true",
			"beforeClean__test.cypher,test,true",
			"beforeclean__test.cypher,n/a,false",
			"afterMigrate.cyp,n/a,false"
	}, nullValues = "n/a")
	void patternShouldWork(String in, String description, boolean matches) {
		Matcher m = LifecyclePhase.LIFECYCLE_PATTERN.matcher(in);
		if (matches) {
			assertThat(m.matches()).isTrue();
			if (description != null) {
				assertThat(m.group(2)).isEqualTo(description);
			} else {
				assertThat(m.group(2)).isNull();
			}
		} else {
			assertThat(m.matches()).isFalse();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"my/awesome/callbacks/afterMigrate.cypher",
		"my/awesome/callbacks/afterConnect.cypher",
		"my/awesome/callbacks/afterConnect__anotherStep.cypher"
	})
	void ifItCanParseItShouldMatch(String in) {
		Matcher matcher = LifecyclePhase.LIFECYCLE_PATTERN.matcher(in.substring(in.lastIndexOf("/") + 1));
		if (LifecyclePhase.canParse(in)) {
			assertThat(matcher.matches()).isTrue();
		} else {
			assertThat(matcher.matches()).isFalse();
		}
	}
}
