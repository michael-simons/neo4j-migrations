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
package ac.simons.neo4j.migrations.core.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Michael J. Simons
 */
class ConstraintToXMLRendererTest {

	static Stream<Arguments> shouldRenderConstraintsToXML() {
		return Stream.of(
			Arguments.of(Named.of("node property exists",
					new Constraint("name", Constraint.Type.EXISTS, TargetEntity.NODE, "Book", Collections.singleton("isbn"),
						"")),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<constraint id=\"name\" type=\"exists\">\n"
				+ "    <label>Book</label>\n"
				+ "    <properties>\n"
				+ "        <property>isbn</property>\n"
				+ "    </properties>\n"
				+ "</constraint>"
			),
			Arguments.of(Named.of("relationship property exists",
					new Constraint("name", Constraint.Type.EXISTS, TargetEntity.RELATIONSHIP, "LIKED",
						Collections.singleton("day"), "")),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<constraint id=\"name\" type=\"exists\">\n"
				+ "    <type>LIKED</type>\n"
				+ "    <properties>\n"
				+ "        <property>day</property>\n"
				+ "    </properties>\n"
				+ "</constraint>"
			),
			Arguments.of(Named.of("node property is unique",
					new Constraint("name", Constraint.Type.UNIQUE, TargetEntity.NODE, "Book", Collections.singleton("isbn"),
						"")),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<constraint id=\"name\" type=\"unique\">\n"
				+ "    <label>Book</label>\n"
				+ "    <properties>\n"
				+ "        <property>isbn</property>\n"
				+ "    </properties>\n"
				+ "</constraint>"
			),
			Arguments.of(Named.of("node key",
					new Constraint("name", Constraint.Type.KEY, TargetEntity.NODE, "Person",
						Arrays.asList("firstname", "surname"), "")),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<constraint id=\"name\" type=\"key\">\n"
				+ "    <label>Person</label>\n"
				+ "    <properties>\n"
				+ "        <property>firstname</property>\n"
				+ "        <property>surname</property>\n"
				+ "    </properties>\n"
				+ "</constraint>"
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void shouldRenderConstraintsToXML(Constraint constraint, String expected) {

		assertThat(new ConstraintToXMLRenderer().render(constraint, RenderContext.defaultContext()).trim()).isEqualTo(
			expected);
	}
}
