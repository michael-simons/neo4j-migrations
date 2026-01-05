/*
 * Copyright 2020-2026 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
class IndexToXMLRendererTests {

	static Stream<Arguments> shouldRenderIndexToXML() {
		return Stream
			.of(Arguments
				.of(Named.of("node property index",
						new Index("name", Index.Type.PROPERTY, TargetEntityType.NODE, Collections.singleton("Book"),
								Collections.singleton("isbn"))),
						"""
								<?xml version="1.0" encoding="UTF-8" standalone="no"?>
								<index name="name" type="property">
								    <label>Book</label>
								    <properties>
								        <property>isbn</property>
								    </properties>
								</index>"""),
					Arguments.of(Named.of("multiple node property indexes", new Index("name", Index.Type.PROPERTY,
							TargetEntityType.NODE, Collections.singleton("Book"), Arrays.asList("isbn", "title"))), """
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="property">
									    <label>Book</label>
									    <properties>
									        <property>isbn</property>
									        <property>title</property>
									    </properties>
									</index>"""),
					Arguments.of(Named.of("relationship property index",
							new Index("name", Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
									Collections.singleton("READS"), Collections.singleton("property"))),
							"""
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="property">
									    <type>READS</type>
									    <properties>
									        <property>property</property>
									    </properties>
									</index>"""),
					Arguments.of(
							Named.of("multiple relationship property indexes",
									new Index("name", Index.Type.PROPERTY, TargetEntityType.RELATIONSHIP,
											Collections.singleton("READS"), Arrays.asList("property1", "property2"))),
							"""
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="property">
									    <type>READS</type>
									    <properties>
									        <property>property1</property>
									        <property>property2</property>
									    </properties>
									</index>"""),
					Arguments.of(
							Named.of("node fulltext index",
									new Index("name", Index.Type.FULLTEXT, TargetEntityType.NODE,
											Collections.singleton("Book"), Arrays.asList("property1", "property2"))),
							"""
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="fulltext">
									    <label>Book</label>
									    <properties>
									        <property>property1</property>
									        <property>property2</property>
									    </properties>
									</index>"""),
					Arguments.of(
							Named.of("piped labels",
									new Index("name", Index.Type.FULLTEXT, TargetEntityType.NODE,
											Collections.singleton("Bo|ok"), Arrays.asList("property1", "property2"))),
							"""
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="fulltext">
									    <label>Bo\\|ok</label>
									    <properties>
									        <property>property1</property>
									        <property>property2</property>
									    </properties>
									</index>"""),
					Arguments.of(
							Named
								.of("multiple labels",
										new Index("name", Index.Type.FULLTEXT, TargetEntityType.NODE,
												Arrays.asList("Bo", "ok"), Arrays.asList("property1", "property2"))),
							"""
									<?xml version="1.0" encoding="UTF-8" standalone="no"?>
									<index name="name" type="fulltext">
									    <label>Bo|ok</label>
									    <properties>
									        <property>property1</property>
									        <property>property2</property>
									    </properties>
									</index>"""));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void shouldRenderIndexToXML(Index index, String expected) {

		assertThat(Renderer.get(Renderer.Format.XML, Index.class).render(index, RenderConfig.defaultConfig()).trim())
			.isEqualTo(expected);
	}

}
