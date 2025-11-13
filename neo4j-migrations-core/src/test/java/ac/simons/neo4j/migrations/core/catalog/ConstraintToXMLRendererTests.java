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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class ConstraintToXMLRendererTests {

	@SuppressWarnings("checkstyle:RegexpSinglelineJava")
	static Stream<Arguments> shouldRenderConstraintsToXML() {
		return Stream.of(
				Arguments.of(Named.of("node property exists",
						new Constraint("name", Constraint.Type.EXISTS, TargetEntityType.NODE, "Book", List.of("isbn"),
								"", null)),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="exists">
								    <label>Book</label>
								    <properties>
								        <property>isbn</property>
								    </properties>
								</constraint>"""),
				Arguments.of(Named.of("relationship property exists",
						new Constraint("name", Constraint.Type.EXISTS, TargetEntityType.RELATIONSHIP, "LIKED",
								List.of("day"), "", null)),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="exists">
								    <type>LIKED</type>
								    <properties>
								        <property>day</property>
								    </properties>
								</constraint>"""),
				Arguments.of(Named.of("node property is unique",
						new Constraint("name", Constraint.Type.UNIQUE, TargetEntityType.NODE, "Book", List.of("isbn"),
								"", null)),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="unique">
								    <label>Book</label>
								    <properties>
								        <property>isbn</property>
								    </properties>
								</constraint>"""),
				Arguments.of(Named.of("node key",
						new Constraint("name", Constraint.Type.KEY, TargetEntityType.NODE, "Person",
								List.of("firstname", "surname"), "", null)),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="key">
								    <label>Person</label>
								    <properties>
								        <property>firstname</property>
								        <property>surname</property>
								    </properties>
								</constraint>"""),
				Arguments.of(Named.of("node property type",
						new Constraint("name", Constraint.Type.PROPERTY_TYPE, TargetEntityType.NODE, "Person",
								List.of("name"), "", PropertyType.STRING)),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="property_type">
								    <label>Person</label>
								    <properties>
								        <property type="STRING">name</property>
								    </properties>
								</constraint>"""),
				Arguments.of(Named.of("rel property type", new Constraint("name", Constraint.Type.PROPERTY_TYPE,
						TargetEntityType.RELATIONSHIP, "LIKED", List.of("name"), "", PropertyType.LOCAL_DATETIME)), """
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<constraint name="name" type="property_type">
								    <type>LIKED</type>
								    <properties>
								        <property type="LOCAL DATETIME">name</property>
								    </properties>
								</constraint>"""));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void shouldRenderConstraintsToXML(Constraint constraint, String expected) {

		assertThat(Renderer.get(Renderer.Format.XML, Constraint.class)
			.render(constraint, RenderConfig.defaultConfig())
			.trim()).isEqualTo(expected);
	}

}
