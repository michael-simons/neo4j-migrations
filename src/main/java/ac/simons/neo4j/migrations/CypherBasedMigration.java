package ac.simons.neo4j.migrations;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.neo4j.driver.Driver;

final class CypherBasedMigration implements Migration {

	private final URL url;
	private final String script;
	private final String description;
	private final MigrationVersion version;

	CypherBasedMigration(URL url) {

		this.url = url;
		String path = this.url.getPath();
		try {
			path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Somethings broken: UTF-8 encoding not supported.");
		}
		int lastIndexOf = path.lastIndexOf("/");
		this.script = lastIndexOf < 0 ? path : path.substring(lastIndexOf + 1);
		this.version = MigrationVersion.parse(this.script);
		this.description = this.version.getDescription();
	}

	@Override
	public MigrationVersion getVersion() {
		return version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MigrationType getType() {
		return MigrationType.CYPHER;
	}

	@Override
	public String getSource() {
		return this.script;
	}

	@Override
	public void apply(Driver driver) throws Exception {

	}
}
