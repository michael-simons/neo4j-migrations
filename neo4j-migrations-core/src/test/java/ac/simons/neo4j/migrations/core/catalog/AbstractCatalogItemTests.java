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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
class AbstractCatalogItemTests {

	// GH-656
	@Test
	void shouldHandleAbsentOptions() {

		MapAccessor row = Mockito.mock(MapAccessor.class);
		given(row.containsKey("options")).willReturn(false);
		assertThat(AbstractCatalogItem.resolveOptions(row)).isEmpty();
		verify(row).containsKey("options");
		verifyNoMoreInteractions(row);
	}

	// GH-656
	@Test
	void shouldHandleEmptyOptions() {

		MapAccessor row = Mockito.mock(MapAccessor.class);
		given(row.containsKey("options")).willReturn(true);
		given(row.get("options")).willReturn(Values.value(Collections.emptyMap()));
		assertThat(AbstractCatalogItem.resolveOptions(row)).isEmpty();
		verify(row).containsKey("options");
		verify(row).get("options");
		verifyNoMoreInteractions(row);
	}

	// GH-656
	@Test
	void shouldRenderHalfWayDecentMaps() {

		Map<String, Value> options = new HashMap<>();
		options.put("v1", Values
			.value(Collections.singletonMap("a", Collections.singletonList(Collections.singletonMap("x", "y")))));
		options.put("v2", Values.value(1, 2, 3));
		options.put("v3", Values.value(true));
		options.put("v4", Values.value("true"));

		MapAccessor row = Mockito.mock(MapAccessor.class);
		given(row.containsKey("options")).willReturn(true);
		given(row.get("options")).willReturn(Values.value(options));
		assertThat(AbstractCatalogItem.resolveOptions(row))
			.hasValue("{`v1`: {`a`: [{`x`: \"y\"}]}, `v2`: [1, 2, 3], `v3`: TRUE, `v4`: \"true\"}");
		verify(row).containsKey("options");
		verify(row).get("options");
		verifyNoMoreInteractions(row);
	}

	@Test
	void defaultMethodsShouldThrow() {

		var item = new AbstractCatalogItem<>("x", Constraint.Type.EXISTS, TargetEntityType.NODE, "x", List.of("x"),
				null) {
		};

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> item.withName("x"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> item.isEquivalentTo(null));
	}

}
