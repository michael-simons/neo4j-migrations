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
import java.util.List;

import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class RenderConfigTests {

	@Test
	void ifNotExistsShouldWork() {

		RenderConfig ctx = RenderConfig.create().ifNotExists().forVersionAndEdition("4.0", "ENTERPRISE");
		assertThat(ctx.getOperator()).isEqualTo(Operator.CREATE);
		assertThat(ctx.isIdempotent()).isTrue();
		assertThat(ctx.getVersion()).isEqualTo(Neo4jVersion.V4_0);
		assertThat(ctx.getEdition()).isEqualTo(Neo4jEdition.ENTERPRISE);
	}

	@Test
	void ifExistsShouldWork() {

		RenderConfig ctx = RenderConfig.drop().ifExists().forVersionAndEdition("4.0", "ENTERPRISE");
		assertThat(ctx.getOperator()).isEqualTo(Operator.DROP);
		assertThat(ctx.isIdempotent()).isTrue();
		assertThat(ctx.getVersion()).isEqualTo(Neo4jVersion.V4_0);
		assertThat(ctx.getEdition()).isEqualTo(Neo4jEdition.ENTERPRISE);
	}

	@Test
	void nonIdempotentShouldBeOk() {

		RenderConfig ctx = RenderConfig.drop().forVersionAndEdition("4.0", "ENTERPRISE");
		assertThat(ctx.getOperator()).isEqualTo(Operator.DROP);
		assertThat(ctx.isIdempotent()).isFalse();
		assertThat(ctx.getVersion()).isEqualTo(Neo4jVersion.V4_0);
		assertThat(ctx.getEdition()).isEqualTo(Neo4jEdition.ENTERPRISE);
	}

	@Test
	void ignoreNameShouldWork() {

		RenderConfig ctx = RenderConfig.drop().forVersionAndEdition("4.0", "ENTERPRISE");
		RenderConfig ctx2 = ctx.ignoreName();

		assertThat(ctx2.getOperator()).isEqualTo(Operator.DROP);
		assertThat(ctx2.isIdempotent()).isFalse();
		assertThat(ctx2.getVersion()).isEqualTo(Neo4jVersion.V4_0);
		assertThat(ctx2.getEdition()).isEqualTo(Neo4jEdition.ENTERPRISE);

		assertThat(ctx.isIgnoreName()).isFalse();
		assertThat(ctx2.isIgnoreName()).isTrue();
	}

	@Test
	void addingNewOptionsShouldWork() {

		RenderConfig ctx = RenderConfig.drop().forVersionAndEdition("4.0", "ENTERPRISE");
		List<RenderConfig.XMLRenderingOptions> newOptions = Collections
			.singletonList(Mockito.mock(RenderConfig.XMLRenderingOptions.class));

		RenderConfig newConfig = ctx.withAdditionalOptions(newOptions);
		assertThat(newConfig).isNotSameAs(ctx);
		assertThat(newConfig.getAdditionalOptions()).hasSize(1);

		RenderConfig newConfig2 = newConfig.withAdditionalOptions(newOptions);
		assertThat(newConfig2).isSameAs(newConfig);

		RenderConfig newConfig3 = newConfig.withAdditionalOptions(null);
		assertThat(newConfig3).isNotSameAs(newConfig);
		assertThat(newConfig3.getAdditionalOptions()).isEmpty();

		newConfig3 = newConfig.withAdditionalOptions(Collections.emptyList());
		assertThat(newConfig3).isNotSameAs(newConfig);
		assertThat(newConfig3.getAdditionalOptions()).isEmpty();

		newConfig3 = newConfig
			.withAdditionalOptions(Collections.singletonList(Mockito.mock(RenderConfig.XMLRenderingOptions.class)));
		assertThat(newConfig3).isNotSameAs(newConfig);
		assertThat(newConfig3.getAdditionalOptions()).hasSize(1);
	}

}
