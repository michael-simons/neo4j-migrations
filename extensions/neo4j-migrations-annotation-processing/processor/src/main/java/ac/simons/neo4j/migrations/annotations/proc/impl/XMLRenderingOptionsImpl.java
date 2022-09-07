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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import ac.simons.neo4j.migrations.core.catalog.RenderConfig;

import java.util.Optional;

/**
 * Default implementation to be passed to the renderer
 */
final class XMLRenderingOptionsImpl implements RenderConfig.XMLRenderingOptions {

	private final boolean withApply;

	private final boolean withReset;

	private final String header;

	XMLRenderingOptionsImpl(boolean withApply, boolean withReset, String header) {
		this.withApply = withApply;
		this.withReset = withReset;
		this.header = header;
	}

	@Override
	public boolean withApply() {
		return withApply;
	}

	@Override
	public boolean withReset() {
		return withReset;
	}

	@Override
	public Optional<String> optionalHeader() {
		return Optional.ofNullable(header);
	}
}
