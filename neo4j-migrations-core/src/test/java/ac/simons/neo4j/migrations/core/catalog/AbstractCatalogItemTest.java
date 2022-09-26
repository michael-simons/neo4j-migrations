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
package ac.simons.neo4j.migrations.core.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;

/**
 * @author Michael J. Simons
 */
class AbstractCatalogItemTest {

	@Test // GH-656
	void shouldHandleAbsentOptions() {

		MapAccessor row = Mockito.mock(MapAccessor.class);
		when(row.containsKey("options")).thenReturn(false);
		assertThat(AbstractCatalogItem.resolveOptions(row)).isEmpty();
		verify(row).containsKey("options");
		verifyNoMoreInteractions(row);
	}

	@Test // GH-656
	void shouldHandleEmptyOptions() {

		MapAccessor row = Mockito.mock(MapAccessor.class);
		when(row.containsKey("options")).thenReturn(true);
		when(row.get("options")).thenReturn(Values.value(Collections.emptyMap()));
		assertThat(AbstractCatalogItem.resolveOptions(row)).isEmpty();
		verify(row).containsKey("options");
		verify(row).get("options");
		verifyNoMoreInteractions(row);
	}

	@Test // GH-656
	void shouldRenderHalfWayDecentMaps() {

		Map<String, Value> options = new HashMap<>();
		options.put("v1",
			Values.value(Collections.singletonMap("a", Collections.singletonList(Collections.singletonMap("x", "y")))));
		options.put("v2", Values.value(1, 2, 3));
		options.put("v3", Values.value(true));
		options.put("v4", Values.value("true"));

		MapAccessor row = Mockito.mock(MapAccessor.class);
		when(row.containsKey("options")).thenReturn(true);
		when(row.get("options")).thenReturn(Values.value(options));
		assertThat(AbstractCatalogItem.resolveOptions(row)).hasValue(
			"{`v1`: {`a`: [{`x`: \"y\"}]}, `v2`: [1, 2, 3], `v3`: TRUE, `v4`: \"true\"}");
		verify(row).containsKey("options");
		verify(row).get("options");
		verifyNoMoreInteractions(row);
	}
}
