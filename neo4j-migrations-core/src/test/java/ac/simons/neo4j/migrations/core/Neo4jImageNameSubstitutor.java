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
package ac.simons.neo4j.migrations.core;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * Changes the source repository to aarch64 compatible images for neo4j < 4.4
 */
public class Neo4jImageNameSubstitutor extends ImageNameSubstitutor {

	@Override
	public DockerImageName apply(DockerImageName dockerImageName) {
		// don't change ryuk and neo4j 4.4+ coordinates
		if (!dockerImageName.getRepository().startsWith("neo4j") || (dockerImageName.getRepository().startsWith("neo4j") && dockerImageName.getVersionPart().contains("4.4"))) {
			return dockerImageName;
		}
		String dockerArchitecture = DockerClientFactory.instance().getInfo().getArchitecture();
		if ("aarch64".equals(dockerArchitecture)) {
			return DockerImageName.parse("neo4j/neo4j-arm64-experimental:" + dockerImageName.getVersionPart() + "-arm64");
		}
		return dockerImageName;
	}

	@Override
	protected String getDescription() {
		return "Neo4j Image name substitutor";
	}
}
