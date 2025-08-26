///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.neo4j.driver:neo4j-java-driver:4.4.20
import java.time.Duration;
import java.util.Objects;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.Neo4jException;

public class check_availability {

	public static void main(String... args) throws InterruptedException {

		if (args.length < 2) {
			throw new IllegalArgumentException("Required arguments are <NAME_OF_URL_VAR> <NAME_OF_PWD_VAR>");
		}

		if (args.length == 3) {
			Thread.sleep(Duration.ofSeconds(Integer.parseInt(args[2])).toMillis());
		}

		var url = Objects.requireNonNull(System.getenv(args[0]));
		var pwd = Objects.requireNonNull(System.getenv(args[1]));
		try (var driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", pwd))) {
			driver.verifyConnectivity();
			System.out.println("Database available.");
		} catch (Neo4jException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		System.exit(0);
	}
}
