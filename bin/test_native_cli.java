///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.testcontainers:neo4j:1.16.2
//DEPS org.slf4j:slf4j-simple:1.7.32
//DEPS org.neo4j.driver:neo4j-java-driver:4.4.2
//DEPS org.assertj:assertj-core:3.22.0
package ac.simons.neo4j.migrations.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Needs to be called from the root of the project.
 */
public class test_native_cli {

	public static void main(String... a) throws Exception {

		var executable = Paths.get("./neo4j-migrations-cli/target/neo4j-migrations").toAbsolutePath().normalize().toString();
		var scripts = "file://" + Paths.get("./neo4j-migrations-test-resources/src/main/resources/some/changeset").toAbsolutePath().normalize();

		// Let Ryuk take care of it, so no try/catch with autockose
		var reusable = TestcontainersConfiguration.getInstance().environmentSupportsReuse();
		var neo4j = new Neo4jContainer<>("neo4j:4.3").withReuse(reusable);
		neo4j.start();

		try (var driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()));
			var session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
		}

		var expectedOutput = List.of(
			"Applied migration 0001 (\"delete old data\")",
			"Applied migration 0002 (\"create new data\")",
			"Database migrated to version 0002."
		);

		var p = new ProcessBuilder(executable,
			"--address", neo4j.getBoltUrl(),
			"--password", neo4j.getAdminPassword(),
			"--location", scripts,
			"apply"
		).start();

		p.onExit().thenAccept(done -> {
			try (var in = new BufferedReader(new InputStreamReader(done.getInputStream()))) {
				var output = in.lines().collect(Collectors.toCollection(LinkedHashSet::new));
				assertThat(output).containsExactlyElementsOf(expectedOutput);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).get();
	}
}
