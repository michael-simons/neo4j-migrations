///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.testcontainers:testcontainers-neo4j:2.0.1
//DEPS org.slf4j:slf4j-simple:1.7.36
//DEPS org.neo4j.driver:neo4j-java-driver:5.28.10
//DEPS org.assertj:assertj-core:3.27.4
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * Needs to be called from the root of the project.
 */
public class test_native_cli {

	public static void main(String... a) throws Exception {

		var imagePattern = Pattern.compile("\\s+<neo4j\\.image>(.+)</neo4j\\.image>");
		var imageName = Files.readAllLines(Paths.get("./pom.xml"))
			.stream()
			.map(imagePattern::matcher)
			.filter(Matcher::matches)
			.map(m -> m.group(1))
			.findFirst()
			.orElse("neo4j:4:4");

		var executable = Paths.get("./neo4j-migrations-cli/target/neo4j-migrations").toAbsolutePath().normalize().toString();
		var location1 = Paths.get("./neo4j-migrations-test-resources/src/main/resources/some/changeset").toAbsolutePath().normalize().toUri().toString();
		var location2 = Paths.get("./neo4j-migrations-test-resources/src/main/resources/catalogbased_changesets").toAbsolutePath().normalize().toUri().toString();

		// Let Ryuk take care of it, so no try/catch with autoclose
		var neo4j = new Neo4jContainer(imageName).withReuse(true);
		neo4j.start();

		try (var driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", neo4j.getAdminPassword()));
			var session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
		}

		var expectedOutput = List.of(
			"Applied migration 0001 (\"delete old data\").",
			"Applied migration 0002 (\"create new data\").",
			"Applied migration 0003 (\"Create constraints\").",
			"Database migrated to version 0003."
		);

		var p = new ProcessBuilder(executable,
			"--address", neo4j.getBoltUrl(),
			"--password", neo4j.getAdminPassword(),
			"--location", location1,
			"--location", location2,
			"apply"
		).redirectErrorStream(true).start();

		p.onExit().thenAccept(done -> {
			try (var in = new BufferedReader(new InputStreamReader(done.getInputStream()))) {
				var output = in.lines()
					.filter(line -> !line.startsWith("WARNING: "))
					.collect(Collectors.toCollection(LinkedHashSet::new));
				assertThat(output).allMatch(c -> expectedOutput.stream().anyMatch(c::contains));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).get();
	}
}
