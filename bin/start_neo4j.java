///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//JAVA_OPTIONS --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED
//DEPS org.neo4j:neo4j:4.4.44
//DEPS org.neo4j:neo4j-bolt:4.4.44
import java.io.IOException;
import java.nio.file.Files;

import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

public class start_neo4j {

	public static void main(String... args) throws IOException {

		new DatabaseManagementServiceBuilder(Files.createTempDirectory("neo4j"))
			.setConfig(BoltConnector.enabled, true)
			.setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
			.build();
		System.out.println("Neo4j (embedded) started.");
	}
}
