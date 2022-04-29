package ac.simons.neo4j.migrations.core.catalog;

import ac.simons.neo4j.migrations.core.Neo4jEdition;

abstract class AbstractContext {

	/**
	 * Neo4j version used to get any of the contained information.
	 */
	private final Version version;

	/**
	 * Neo4j edition used to get any of the contained information.
	 */
	private final Neo4jEdition edition;

	protected AbstractContext(String version, Neo4jEdition edition) {
		this.version = Version.of(version);
		this.edition = edition;
	}

	public Version getVersion() {
		return version;
	}

	public Neo4jEdition getEdition() {
		return edition;
	}
}
