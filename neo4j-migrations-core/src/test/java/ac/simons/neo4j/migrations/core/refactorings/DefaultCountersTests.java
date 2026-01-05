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
package ac.simons.neo4j.migrations.core.refactorings;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.summary.SummaryCounters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
class DefaultCountersTests {

	@Test
	void shouldConvertSummaryCounters() {
		SummaryCounters summaryCounters = mock(SummaryCounters.class);
		given(summaryCounters.nodesCreated()).willReturn(1);
		given(summaryCounters.nodesDeleted()).willReturn(2);
		given(summaryCounters.labelsAdded()).willReturn(3);
		given(summaryCounters.labelsRemoved()).willReturn(4);
		given(summaryCounters.relationshipsCreated()).willReturn(5);
		given(summaryCounters.relationshipsDeleted()).willReturn(6);
		given(summaryCounters.indexesAdded()).willReturn(7);
		given(summaryCounters.indexesRemoved()).willReturn(8);
		given(summaryCounters.constraintsAdded()).willReturn(9);
		given(summaryCounters.constraintsRemoved()).willReturn(10);
		given(summaryCounters.propertiesSet()).willReturn(11);

		Counters counters = new DefaultCounters(summaryCounters);
		assertThat(counters.nodesCreated()).isEqualTo(1);
		assertThat(counters.nodesDeleted()).isEqualTo(2);
		assertThat(counters.labelsAdded()).isEqualTo(3);
		assertThat(counters.labelsRemoved()).isEqualTo(4);
		assertThat(counters.typesAdded()).isEqualTo(5);
		assertThat(counters.typesRemoved()).isEqualTo(6);
		assertThat(counters.indexesAdded()).isEqualTo(7);
		assertThat(counters.indexesRemoved()).isEqualTo(8);
		assertThat(counters.constraintsAdded()).isEqualTo(9);
		assertThat(counters.constraintsRemoved()).isEqualTo(10);
		assertThat(counters.propertiesSet()).isEqualTo(11);
	}

	@Test
	void shouldAdd() {

		SummaryCounters summaryCounters = mock(SummaryCounters.class);
		given(summaryCounters.nodesCreated()).willReturn(1);
		given(summaryCounters.nodesDeleted()).willReturn(2);
		given(summaryCounters.labelsAdded()).willReturn(3);
		given(summaryCounters.labelsRemoved()).willReturn(4);
		given(summaryCounters.relationshipsCreated()).willReturn(5);
		given(summaryCounters.relationshipsDeleted()).willReturn(6);
		given(summaryCounters.propertiesSet()).willReturn(7);
		given(summaryCounters.indexesAdded()).willReturn(8);
		given(summaryCounters.indexesRemoved()).willReturn(9);
		given(summaryCounters.constraintsAdded()).willReturn(10);
		given(summaryCounters.constraintsRemoved()).willReturn(11);

		Counters counters = new DefaultCounters(summaryCounters).add(new DefaultCounters(summaryCounters));
		assertThat(counters.nodesCreated()).isEqualTo(2);
		assertThat(counters.nodesDeleted()).isEqualTo(4);
		assertThat(counters.labelsAdded()).isEqualTo(6);
		assertThat(counters.labelsRemoved()).isEqualTo(8);
		assertThat(counters.typesAdded()).isEqualTo(10);
		assertThat(counters.typesRemoved()).isEqualTo(12);
		assertThat(counters.propertiesSet()).isEqualTo(14);
		assertThat(counters.indexesAdded()).isEqualTo(16);
		assertThat(counters.indexesRemoved()).isEqualTo(18);
		assertThat(counters.constraintsAdded()).isEqualTo(20);
		assertThat(counters.constraintsRemoved()).isEqualTo(22);
	}

	@Test
	void shouldNoopEmpty() {

		SummaryCounters summaryCounters = mock(SummaryCounters.class);
		given(summaryCounters.nodesCreated()).willReturn(1);
		given(summaryCounters.nodesDeleted()).willReturn(2);
		given(summaryCounters.labelsAdded()).willReturn(3);
		given(summaryCounters.labelsRemoved()).willReturn(4);
		given(summaryCounters.relationshipsCreated()).willReturn(5);
		given(summaryCounters.relationshipsDeleted()).willReturn(6);
		given(summaryCounters.propertiesSet()).willReturn(7);
		given(summaryCounters.indexesAdded()).willReturn(8);
		given(summaryCounters.indexesRemoved()).willReturn(9);
		given(summaryCounters.constraintsAdded()).willReturn(10);
		given(summaryCounters.constraintsRemoved()).willReturn(11);

		Counters original = new DefaultCounters(summaryCounters);
		Counters counters = original.add(Counters.Empty.INSTANCE);
		assertThat(counters).isSameAs(original);
	}

	@Test
	void shouldAddThingsToEmpty() {

		SummaryCounters summaryCounters = mock(SummaryCounters.class);
		given(summaryCounters.nodesCreated()).willReturn(1);
		given(summaryCounters.nodesDeleted()).willReturn(2);
		given(summaryCounters.labelsAdded()).willReturn(3);
		given(summaryCounters.labelsRemoved()).willReturn(4);
		given(summaryCounters.relationshipsCreated()).willReturn(5);
		given(summaryCounters.relationshipsDeleted()).willReturn(6);
		given(summaryCounters.propertiesSet()).willReturn(7);
		given(summaryCounters.indexesAdded()).willReturn(8);
		given(summaryCounters.indexesRemoved()).willReturn(9);
		given(summaryCounters.constraintsAdded()).willReturn(10);
		given(summaryCounters.constraintsRemoved()).willReturn(11);

		Counters counters = Counters.empty().add(new DefaultCounters(summaryCounters));
		assertThat(counters.nodesCreated()).isEqualTo(1);
		assertThat(counters.nodesDeleted()).isEqualTo(2);
		assertThat(counters.labelsAdded()).isEqualTo(3);
		assertThat(counters.labelsRemoved()).isEqualTo(4);
		assertThat(counters.typesAdded()).isEqualTo(5);
		assertThat(counters.typesRemoved()).isEqualTo(6);
		assertThat(counters.propertiesSet()).isEqualTo(7);
		assertThat(counters.indexesAdded()).isEqualTo(8);
		assertThat(counters.indexesRemoved()).isEqualTo(9);
		assertThat(counters.constraintsAdded()).isEqualTo(10);
		assertThat(counters.constraintsRemoved()).isEqualTo(11);
	}

}
