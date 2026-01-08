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
package ac.simons.neo4j.migrations.core;

import java.util.Set;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * Changes the source repository to aarch64 compatible images for neo4j < 4.4
 *
 * @author Gerrit Meier
 */
public final class Neo4jImageNameSubstitutor extends ImageNameSubstitutor {

	private final Set<String> OFFICIALLY_SUPPORTED_ON_ARM = Set.of("4.4", "5", "2025");

	@Override
	public DockerImageName apply(DockerImageName dockerImageName) {

		// don't change ryuk and neo4j 4.4+ coordinates
		var isNeo4jRepo = dockerImageName.getRepository().startsWith("neo4j");
		var versionPart = dockerImageName.getVersionPart();
		if (!isNeo4jRepo || this.OFFICIALLY_SUPPORTED_ON_ARM.stream().anyMatch(versionPart::startsWith)) {
			return dockerImageName;
		}
		if (versionPart.startsWith("LATEST")) {
			return DockerImageName.parse(dockerImageName.getRepository() + ":" + versionPart.replace("LATEST", "2025"));
		}

		String dockerArchitecture = DockerClientFactory.instance().getInfo().getArchitecture();
		if ("aarch64".equals(dockerArchitecture)) {
			var version = dockerImageName.getVersionPart();
			if (version.equals("4.0")) {
				version = "4.0.12";
			}
			return DockerImageName.parse("neo4j/neo4j-arm64-experimental:" + version + "-arm64");
		}
		return dockerImageName;
	}

	@Override
	protected String getDescription() {
		return "Neo4j Image name substitutor";
	}

}
