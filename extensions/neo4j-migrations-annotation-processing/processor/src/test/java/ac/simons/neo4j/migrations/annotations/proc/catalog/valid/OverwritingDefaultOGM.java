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
package ac.simons.neo4j.migrations.annotations.proc.catalog.valid;

import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;

import ac.simons.neo4j.migrations.annotations.catalog.Unique;

/**
 * @author Michael J. Simons
 */
@NodeEntity
public class OverwritingDefaultOGM {

	@Unique(label = "whatever")
	public UUID uuid;
}
